package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
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
    private static final Logger LOGGER = Logger.getLogger(ShipmentSubscriber.class.getName());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    @Inject
    private NamedCache<String, Order> orders;

    @Inject
    private NamedTopic<Order> ordersTopic;
    private Subscriber<Order> subscriber;

    @Inject
    @GrpcChannel(name = "shipping")
    @GrpcServiceProxy
    private ShippingService shippingService;

    public ShipmentSubscriber() {
    }

    ShipmentSubscriber(NamedCache<String, Order> orders, NamedTopic<Order> ordersTopic, ShippingService service) {
        this.orders = orders;
        this.ordersTopic = ordersTopic;
        this.shippingService = service;

        ensureRunning();
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    protected void ensureRunning() {
        LOGGER.info("Configuring ShipmentSubscriber");
        subscriber = ordersTopic.createSubscriber(
                Subscriber.Name.of("shipment"),
                Subscriber.Filtered.by(Filters.equal(Order::getStatus, Order.Status.PAID)));
        EXECUTOR.submit(this);
    }

    @Override
    public void run() {
        LOGGER.info("Running ShipmentSubscriber");
        try (Subscriber<Order> subscriber = this.subscriber) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    LOGGER.info("Waiting for PAID orders");
                    Subscriber.Element<Order> element = subscriber.receive().get();

                    Order order = element == null ? null : element.getValue();
                    LOGGER.info("ShipmentSubscriber| Order received: " + order);
                    if (order != null) {

                        // call shipment service
                        Shipment shipment = new Shipment(order.getId());
                        try {
                            // create shipment
                            LOGGER.log(Level.INFO, "Calling Shipping service: " + shipment);

                            Shipment shipmentResponse = shippingService.ship(shipment);
                            LOGGER.log(Level.INFO, "Created Shipment: " + shipmentResponse);

                            orders.invoke(order.getId(), entry ->
                                    entry.setValue(entry.getValue().setShipment(shipmentResponse)));

                            LOGGER.log(Level.INFO, "Updated order: " + orders.get(order.getId()));
                        } catch (Throwable t) {
                            LOGGER.log(Level.SEVERE, "Shipping service call failed: ", t);
                            orders.invoke(order.getId(), entry ->
                                    entry.setValue(entry.getValue().setStatus(Order.Status.SHIPMENT_FAILED)));
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
}
