package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.util.BinaryEntry;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * OrderEventInterceptor is responsible for ...
 *
 * @author hr  2019.09.17
 */
@Interceptor(entryEvents = {Type.INSERTED, Type.UPDATED})
public class OrderEventInterceptor
        implements EventInterceptor<EntryEvent<String, Order>> {
    @Override
    public void onEvent(EntryEvent<String, Order> entryEvent) {
        for (BinaryEntry<String, Order> binEntry : entryEvent.getEntrySet()) {
            Order order = inject(binEntry.getValue());
            order.execute();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T inject(T o) {
        BeanManager bm = CDI.current().getBeanManager();
        Class<T> clz = (Class<T>) o.getClass();
        AnnotatedType<T> annotatedType = bm.createAnnotatedType(clz);
        InjectionTarget<T> injectionTarget = bm.createInjectionTarget(annotatedType);
        CreationalContext<T> context = bm.createCreationalContext(null);
        injectionTarget.inject(o, context);
        injectionTarget.postConstruct(o);
        return o;
    }
}
