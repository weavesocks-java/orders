package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.Session;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.net.topic.Publisher;
import com.tangosol.util.BinaryEntry;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OrderEventInterceptor is responsible for ...
 *
 * @author hr  2019.09.17
 */
@Interceptor(entryEvents = {Type.INSERTED, Type.UPDATED})
public class OrderEventInterceptor
        implements EventInterceptor<EntryEvent<String, Order>>
    {
    @Override
    public void onEvent(EntryEvent<String, Order> entryEvent) {
        for (BinaryEntry<String, Order> binEntry : entryEvent.getEntrySet()) {
            Order order = binEntry.getValue();
            order.setPublisherSupplier(() -> ensurePublisher());

            order.execute();
        }
    }

    protected Publisher<Order> ensurePublisher() {
        Publisher publisher;

        while ((publisher = atomicPublisher.get()) == null) {
            atomicPublisher.compareAndSet(null, Session.create().getTopic("orders-topic").createPublisher());
        }

        return publisher;
    }

    protected AtomicReference<Publisher> atomicPublisher = new AtomicReference<>();

    private static final Logger LOGGER = Logger.getLogger(OrderEventInterceptor.class.getName());
    }
