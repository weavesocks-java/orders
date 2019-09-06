package com.oracle.coherence.weavesocks.order;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/")
public class JaxrsActivator extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(OrderResource.class);
        set.add(OrderExceptionMapper.class);
        return Collections.unmodifiableSet(set);
    }
}