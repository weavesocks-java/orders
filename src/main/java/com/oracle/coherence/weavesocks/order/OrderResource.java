package com.oracle.coherence.weavesocks.order;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.helidon.microprofile.grpc.client.GrpcChannel;
import io.helidon.microprofile.grpc.client.GrpcServiceProxy;

import com.oracle.coherence.weavesocks.order.CustomerService.CustomerRequest;
import com.oracle.coherence.weavesocks.order.CustomerService.CustomerResponse;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filters;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@ApplicationScoped
@Path("/orders")
public class OrderResource {
    private static final Logger LOGGER = Logger.getLogger(OrderResource.class.getName());

    @Inject
    private NamedCache<String, Order> orders;

    @Inject
    @GrpcChannel(name = "user")
    @GrpcServiceProxy
    private CustomerService customerService;

    @Inject
    @GrpcChannel(name = "carts")
    @GrpcServiceProxy
    private CartService cartService;

    @Inject
    @GrpcChannel(name = "payment")
    @GrpcServiceProxy
    private PaymentService paymentService;

    @Inject
    @GrpcChannel(name = "shipping")
    @GrpcServiceProxy
    private ShippingService shippingService;

    @ConfigProperty(name = "http.timeout")
    private long timeout;

    @GET
    @Path("search/customerId")
    @Produces(APPLICATION_JSON)
    public Response getOrdersForCustomer(@QueryParam("custId") String customerId) {
        Collection<Order> customerOrders = orders.values(Filters.equal(Order::getCustomerId, customerId), null);
        if (customerOrders.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        return wrap("customerOrders", customerOrders);
    }

    private Response wrap(String key, Object value) {
        Map<String, Map<String, Object>> map = Collections.singletonMap("_embedded", Collections.singletonMap(key, value));
        return Response.ok(map).build();
    }

    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    public Order getOrder(@PathParam("id") String orderId) {
        return orders.getOrDefault(orderId, new Order());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response newOrder(@Context UriInfo uriInfo, NewOrderRequest request) throws Exception {
        if (request.address == null || request.customer == null || request.card == null || request.items == null) {
            throw new InvalidOrderException("Invalid order request. Order requires customer, address, card and items.");
        }
        LOGGER.log(Level.INFO, "Processing new order: " + request);

        CompletableFuture<CartService.Cart> cartFuture     = cartService.getCart(request.cartId());
        CompletableFuture<CustomerResponse> customerFuture = customerService.getCustomer(new CustomerRequest(request));

        CustomerResponse customer = customerFuture.get();
        LOGGER.log(Level.INFO, "Customer: " + customer);

        CartService.Cart cart = cartFuture.get();
        LOGGER.log(Level.INFO, "Cart: " + cart);

        String orderId = createOrderId();

        Link link = Link.fromMethod(OrderResource.class, "getOrder")
                .baseUri("http://orders/orders/")
                .rel("self")
                .build(orderId);

        Order order = new Order(
                orderId,
                customer.customer,
                customer.address,
                customer.card,
                cart.items);

        order.addLink("self", link);

        // TODO: remove once we have topics and actor interceptor working
        try {
            // Call payment service to make sure they've paid
            PaymentRequest paymentRequest = new PaymentRequest(order);
            LOGGER.log(Level.INFO, "Calling Payment service: " + paymentRequest);

            Payment payment = paymentService.authorize(paymentRequest);
            order.setPayment(payment);
            LOGGER.log(Level.INFO, "Received " + payment);
            if (payment == null) {
                throw new PaymentDeclinedException("Unable to parse authorisation packet");
            }
            if (!payment.authorised) {
                throw new PaymentDeclinedException(payment.message);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Payment service call failed: ", t);
            throw new OrderException(t.getMessage());
        }

        Shipment shipment = new Shipment(orderId);
        try {
            // create shipment
            LOGGER.log(Level.INFO, "Calling Shipping service: " + shipment);

            shipment = shippingService.ship(shipment);
            order.setShipment(shipment);

            LOGGER.log(Level.INFO, "Created Shipment: " + shipment);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Shipping service call failed: ", t);
            throw new OrderException(t.getMessage());
        }

        orders.put(orderId, order);

        LOGGER.log(Level.INFO, "Created Order: " + orderId);
        return Response.status(CREATED).entity(order).build();
    }

    private String createOrderId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static class OrderException extends IllegalStateException {
        public OrderException(String s) {
            super(s);
        }
    }

    public static class InvalidOrderException extends OrderException {
        public InvalidOrderException(String s) {
            super(s);
        }
    }

    public static class PaymentDeclinedException extends OrderException {
        public PaymentDeclinedException(String s) {
            super(s);
        }
    }
}
