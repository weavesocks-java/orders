package com.oracle.coherence.weavesocks.order;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.helidon.common.CollectionsHelper;

import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

@ApplicationScoped
@ApplicationPath("/")
public class JaxrsActivator extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return CollectionsHelper.setOf(OrderResource.class, OrderExceptionMapper.class);
    }

    @Produces
    @ApplicationScoped
    public Session session() {
        return Session.create();
    }

    @Produces
    @ApplicationScoped
    public NamedTopic<Order> ordersTopic(Session session) {
        return session.getTopic("orders-topic");
    }

    @Produces
    @ApplicationScoped
    public Publisher<Order> ordersPublisher(NamedTopic<Order> ordersTopic) {
        return ordersTopic.createPublisher();
    }
}