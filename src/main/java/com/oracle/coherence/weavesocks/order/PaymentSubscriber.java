package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Filters;
import io.helidon.microprofile.grpc.client.GrpcChannel;
import io.helidon.microprofile.grpc.client.GrpcServiceProxy;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.inject.Inject;
import javax.inject.Singleton;
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
public class PaymentSubscriber
        implements Runnable {

    protected AtomicReference<Subscriber> atomicSubscriber = new AtomicReference<>();

    protected Session session;

    @Inject
    private NamedCache<String, Order> orders;

    @Inject
    @GrpcChannel(name = "payment")
    @GrpcServiceProxy
    private PaymentService paymentService;

    public PaymentSubscriber() {
    }

    // constructor for testing
    public PaymentSubscriber(Session session, NamedCache<String, Order> orders, PaymentService service) {
        this.session = session;
        this.orders = orders;
        this.paymentService = service;

        ensureRunning();
    }

    @Override
    public void run() {
        try (Subscriber<Order> subscriber = ensureSubscriber()) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    LOGGER.info("Waiting for created orders");
                    Subscriber.Element<Order> element = subscriber.receive().get();

                    Order order = element == null ? null : element.getValue();

                    if (order != null) {

                        // call payment service
                        try {
                            // Call payment service to make sure they've paid
                            PaymentRequest paymentRequest = new PaymentRequest(order);
                            LOGGER.log(Level.INFO, "Calling Payment service: " + paymentRequest);

                            Payment payment = paymentService.authorize(paymentRequest);

                            LOGGER.log(Level.INFO, "Received " + payment);

                            orders.invoke(order.getId(), entry ->
                                    entry.setValue(entry.getValue().setPayment(payment)));

                        } catch (Throwable t) {
                            LOGGER.log(Level.SEVERE, "Payment service call failed: ", t);
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
                Subscriber.Name.of("payment"),
                Subscriber.Filtered.by(Filters.equal(Order::getStatus, Order.Status.CREATED))};
    }

    public static Subscriber<Order> createSubscriber(Session session) {
        return session.getTopic("orders-topic").createSubscriber(createSubscriptionOptions());
    }


    public static final Logger LOGGER = Logger.getLogger(PaymentSubscriber.class.getName());
    public static final ExecutorService POOL = Executors.newFixedThreadPool(1);
}
