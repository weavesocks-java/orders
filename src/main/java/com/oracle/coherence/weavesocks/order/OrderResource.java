package com.oracle.coherence.weavesocks.order;

import java.net.URI;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.helidon.microprofile.grpc.client.GrpcChannel;
import io.helidon.microprofile.grpc.client.GrpcServiceProxy;

import com.tangosol.net.NamedCache;
import com.tangosol.util.Filters;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@ApplicationScoped
@Path("/orders")
public class OrderResource {

    @Inject
    private NamedCache<String, CustomerOrder> orders;

    @Inject
    @GrpcChannel(name = "payment")
    @GrpcServiceProxy
    private PaymentService paymentService;

    @Inject
    @GrpcChannel(name = "shipping")
    @GrpcServiceProxy
    private ShippingService shippingService;

    private static final Logger LOGGER = Logger.getLogger(OrderResource.class.getName());
    private static final Client CLIENT = ClientBuilder.newClient();

    @ConfigProperty(name = "http.timeout")
    private long timeout;

    @GET
    @Path("search/customerId")
    @Produces(APPLICATION_JSON)
    public Response getOrdersForCustomer(@QueryParam("custId") String customerId) {
        Collection<CustomerOrder> customerOrders = orders.values(Filters.equal(CustomerOrder::getCustomerId, customerId), null);
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
    public CustomerOrder getOrder(@PathParam("id") String orderId) {
        return orders.getOrDefault(orderId, new CustomerOrder());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response newOrder(@Context UriInfo uriInfo, NewOrderRequest request) {
        if (request.address == null || request.customer == null || request.card == null || request.items == null) {
            throw new InvalidOrderException("Invalid order request. Order requires customer, address, card and items.");
        }

        LOGGER.log(Level.INFO, "Processing new order...");

        List<Item> items    = httpGet(request.items, new GenericType<List<Item>>() { });
        Address    address  = httpGet(request.address, Address.class);
        Card       card     = httpGet(request.card, Card.class);
        Customer   customer = httpGet(request.customer, Customer.class);

        float amount = calculateTotal(items);

        String orderId = createOrderId();
        try {
            // Call payment service to make sure they've paid
            PaymentRequest paymentRequest = new PaymentRequest(orderId, address, card, customer, amount);
            LOGGER.log(Level.INFO, "Calling Payment service: " + paymentRequest);

            PaymentResponse paymentResponse = paymentService.authorize(paymentRequest);

            LOGGER.log(Level.INFO, "Received " + paymentResponse);
            if (paymentResponse == null) {
                throw new PaymentDeclinedException("Unable to parse authorisation packet");
            }
            if (!paymentResponse.isAuthorised()) {
                throw new PaymentDeclinedException(paymentResponse.getMessage());
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

            LOGGER.log(Level.INFO, "Created Shipment: " + shipment);
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Shipping service call failed: ", t);
            throw new OrderException(t.getMessage());
        }

        Link link = Link.fromMethod(OrderResource.class, "getOrder")
                .baseUri("http://orders/orders/")
                .rel("self")
                .build(orderId);

        CustomerOrder order = new CustomerOrder(
                orderId,
                customer.getId(),
                customer,
                address,
                card,
                items,
                shipment,
                Calendar.getInstance().getTime(),
                amount);

        order.addLink("self", link);

        orders.put(orderId, order);

        LOGGER.log(Level.INFO, "Created Order: " + orderId);
        return Response.status(CREATED).entity(order).build();
    }

    private String createOrderId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // helper methods

    private float calculateTotal(List<Item> items) {
        float amount = 0F;
        float shipping = 4.99F;
        amount += items.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
        amount += shipping;
        return amount;
    }

    private <T> T httpGet(URI uri, Class<T> responseClass){
        return CLIENT
                .target(uri)
                .request(APPLICATION_JSON)
                .get(responseClass);
    }

    private <T> T httpGet(URI uri, GenericType<T> responseClass){
        return CLIENT
                .target(uri)
                .request(APPLICATION_JSON)
                .get(responseClass);
    }

    // helper classes

    private class Domain {
        private final String domain;

        private Domain(String domain) {
            this.domain = domain;
        }

        @Override
        public String toString() {
            if (domain != null && !domain.equals("")) {
                return "." + domain;
            } else {
                return "";
            }
        }
    }

    private class Hostname {
        private final String hostname;

        private Hostname(String hostname) {
            this.hostname = hostname;
        }

        @Override
        public String toString() {
            if (hostname != null && !hostname.equals("")) {
                return hostname;
            } else {
                return "";
            }
        }
    }

    private class ServiceUri {
        private final Hostname hostname;
        private final Domain domain;
        private final String endpoint;

        private ServiceUri(Hostname hostname, Domain domain, String endpoint) {
            this.hostname = hostname;
            this.domain = domain;
            this.endpoint = endpoint;
        }

        public URI toUri() {
            return URI.create(wrapHTTP(hostname.toString() + domain.toString()) + endpoint);
        }

        private String wrapHTTP(String host) {
            return "http://" + host;
        }

        @Override
        public String toString() {
            return "ServiceUri{" +
                    "hostname=" + hostname +
                    ", domain=" + domain +
                    '}';
        }
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
