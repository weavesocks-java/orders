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
public class PaymentSubscriber
        implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(PaymentSubscriber.class.getName());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    @Inject
    private NamedCache<String, Order> orders;

    @Inject
    private NamedTopic<Order> ordersTopic;
    private Subscriber<Order> subscriber;

    @Inject
    @GrpcChannel(name = "payment")
    @GrpcServiceProxy
    private PaymentService paymentService;

    public PaymentSubscriber() {
    }

    // constructor for testing
    PaymentSubscriber(NamedCache<String, Order> orders, NamedTopic<Order> ordersTopic, PaymentService service) {
        this.orders = orders;
        this.ordersTopic = ordersTopic;
        this.paymentService = service;

        ensureRunning();
    }


    @SuppressWarnings("unchecked")
    @PostConstruct
    protected void ensureRunning() {
        LOGGER.info("Configuring PaymentSubscriber");
        subscriber = ordersTopic.createSubscriber(
                Subscriber.Name.of("payment"),
                Subscriber.Filtered.by(Filters.equal(Order::getStatus, Order.Status.CREATED)));
        EXECUTOR.submit(this);
    }

    @Override
    public void run() {
        LOGGER.info("Running PaymentSubscriber");
        try (Subscriber<Order> subscriber = this.subscriber) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    LOGGER.info("Waiting for CREATED orders");
                    Subscriber.Element<Order> element = subscriber.receive().get();

                    Order order = element == null ? null : element.getValue();
                    LOGGER.info("PaymentSubscriber| Order received: " + order);
                    if (order != null) {
                        try {
                            PaymentRequest paymentRequest = new PaymentRequest(order);
                            LOGGER.log(Level.INFO, "Calling Payment service: " + paymentRequest);

                            Payment payment = paymentService.authorize(paymentRequest);

                            LOGGER.log(Level.INFO, "Received " + payment);

                            orders.invoke(order.getId(), entry ->
                                    entry.setValue(entry.getValue().setPayment(payment)));

                            LOGGER.log(Level.INFO, "Updated order: " + orders.get(order.getId()));
                        } catch (Throwable t) {
                            LOGGER.log(Level.SEVERE, "Payment service call failed: ", t);
                            orders.invoke(order.getId(), entry ->
                                    entry.setValue(entry.getValue().setStatus(Order.Status.PAYMENT_FAILED)));
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.log(Level.SEVERE, "PaymentSubscriber was interrupted");
        }
    }
}
