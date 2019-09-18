package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Filters;
import io.helidon.microprofile.grpc.client.GrpcChannel;
import io.helidon.microprofile.grpc.client.GrpcServiceProxy;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PaymentSubscriber is responsible for ...
 *
 * @author hr  2019.09.17
 */
@ApplicationScoped
public class ShipmentSubscriber
        implements Runnable {

    protected AtomicReference<Subscriber> atomicSubscriber = new AtomicReference<>();

    protected Session session;

    @Inject
    private NamedCache<String, Order> orders;

    @Inject
    @GrpcChannel(name = "shipping")
    @GrpcServiceProxy
    private ShippingService shippingService;

    public ShipmentSubscriber() {
    }

    public ShipmentSubscriber(Session session, NamedCache<String, Order> orders, ShippingService service) {
        this.session = session;
        this.orders = orders;
        this.shippingService = service;

        ensureRunning();
    }

    @Override
    public void run() {
        try (Subscriber<Order> subscriber = ensureSubscriber()) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    LOGGER.info("Waiting for paid orders");
                    Subscriber.Element<Order> element = subscriber.receive().get();

                    Order order = element == null ? null : element.getValue();

                    if (order != null) {

                        // call shipment service
                        Shipment shipment = new Shipment(order.getId());
                        try {
                            // create shipment
                            LOGGER.log(Level.INFO, "Calling Shipping service: " + shipment);

                            Shipment shipmentResponse = shippingService.ship(shipment);

                            orders.invoke(order.getId(), entry ->
                                    entry.setValue(entry.getValue().setShipment(shipmentResponse)));

                            LOGGER.log(Level.INFO, "Created Shipment: " + shipmentResponse);
                        } catch (Throwable t) {
                            LOGGER.log(Level.SEVERE, "Shipping service call failed: ", t);
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.log(Level.SEVERE, "ShipmentSubscriber was interrupted");
        }
    }

    @PostConstruct
    protected void ensureRunning() {
        POOL.submit(this);
        if (session == null) {
            session = SessionFactory.ensureSession();
        }
    }

    protected Subscriber<Order> ensureSubscriber() {
        Subscriber subscriber;
        while ((subscriber = atomicSubscriber.get()) == null) {
            atomicSubscriber.compareAndSet(null, createSubscriber(session));
        }

        return subscriber;
    }

    protected static Subscriber.Option[] createSubscriptionOptions() {
        return new Subscriber.Option[] {
                Subscriber.Name.of("shipment"),
                Subscriber.Filtered.by(Filters.equal(Order::getStatus, Order.Status.PAID))};
    }

    public static Subscriber<Order> createSubscriber(Session session) {
        return session.getTopic("orders-topic").createSubscriber(createSubscriptionOptions());
    }


    public static final Logger LOGGER = Logger.getLogger(ShipmentSubscriber.class.getName());
    public static final ExecutorService POOL = Executors.newFixedThreadPool(1);
}
