package com.oracle.coherence.weavesocks.order;

import io.helidon.config.Config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tangosol.net.NamedCache;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@ApplicationScoped
@Path("/orders")
public class OrderResource {

    @Inject
    private NamedCache<String, CustomerOrder> orders;

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private static final Client client = ClientBuilder.newClient();
    private static final Config config = Config.create();
    private static long timeout = config.get("http.timeout").asLong().get();

    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    public CustomerOrder getOrder(@PathParam("id") String orderId) {

        return orders.getOrDefault(orderId, new CustomerOrder());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public CustomerOrder newOrder(NewOrderResource item) throws Exception {
        try {
            if (item.address == null || item.customer == null ||
                    item.card == null || item.items == null) {
                throw new InvalidOrderException("Invalid order request. Order requires customer, address, card and items.");
            }

            LOGGER.log(Level.INFO, "Processing new order...");

            Future<Address>  addressFuture = asyncGet(item.address, Address.class);
            Future<Customer>  customerFuture = asyncGet(item.customer, Customer.class);
            Future<Card>  cardFuture = asyncGet(item.card, Card.class);
            //Future<List<Item>> itemsFuture = asyncGet(item.items, new ArrayList<Item>() {}.getClass());
            List<Item> items = httpGet(item.items, new ArrayList<Item>() {}.getClass());

            Address address = addressFuture.get(timeout, TimeUnit.SECONDS);
            Card card = cardFuture.get(timeout, TimeUnit.SECONDS);
            Customer customer = customerFuture.get(timeout, TimeUnit.SECONDS);
            float amount = calculateTotal(items);

            // Call payment service to make sure they've paid
            LOGGER.log(Level.INFO, "Calling Payment service ...");

            PaymentRequest paymentRequest = new PaymentRequest(null, address, card, customer, amount);
            URI paymentUri = new ServiceUri(new Hostname("payment"), new Domain(""), "/paymentAuth").toUri();
            Future<Response> paymentResponseFuture = asyncPost(paymentUri, Entity.json(paymentRequest));

            PaymentResponse paymentResponse = paymentResponseFuture.get(timeout, TimeUnit.SECONDS).readEntity(PaymentResponse.class);

            if (paymentResponse == null) {
                throw new PaymentDeclinedException("Unable to parse authorisation packet");
            }
            if (!paymentResponse.isAuthorised()) {
                throw new PaymentDeclinedException(paymentResponse.getMessage());
            }

            // create shipment
            LOGGER.log(Level.INFO, "Creating Shipment ...");

            String customerId = customer.getId();
            String orderId = paymentRequest.getOrderId();
            URI shippingUri = new ServiceUri(new Hostname("shipping"), new Domain(""), "/shipping").toUri();

            Future<Response> shippingResponseFuture = asyncPost(shippingUri, Entity.json(new Shipment(customerId)));

            CustomerOrder order = new CustomerOrder(
                    orderId,
                    customerId,
                    customer,
                    address,
                    card,
                    items,
                    shippingResponseFuture.get(timeout, TimeUnit.SECONDS).readEntity(Shipment.class),
                    Calendar.getInstance().getTime(),
                    amount);

            orders.put(orderId, order);

            LOGGER.log(Level.INFO, "Done processing CustomerOrder id : " + orderId);
            return order;
        } catch (TimeoutException e) {
            throw new IllegalStateException("Unable to create order due to timeout from one of the services.", e);
        }
    }

    // helper methods

    private float calculateTotal(List<Item> items) {
        float amount = 0F;
        float shipping = 4.99F;
        amount += items.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
        amount += shipping;
        return amount;
    }

    public<T> T httpGet(URI uri,Class<T>entityClass){
        return client
                .target(uri)
                .request(APPLICATION_JSON)
                .get(entityClass);
    }

    public <T> Future<T> asyncGet(URI uri, Class<T> entityClass) {
        return client
                .target(uri)
                .request(MediaType.APPLICATION_JSON)
                .async()
                .get(entityClass);
    }

    public Future<Response> asyncPost(URI uri, Entity<?> entity) {
        return client
                .target(uri)
                .request(APPLICATION_JSON)
                .async()
                .post(entity);
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

    public class InvalidOrderException extends IllegalStateException {
        public InvalidOrderException(String s) {
            super(s);
        }
    }

    public class PaymentDeclinedException extends IllegalStateException {
        public PaymentDeclinedException(String s) {
            super(s);
        }
    }
}
