package com.oracle.coherence.weavesocks.order;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.helidon.grpc.core.MarshallerSupplier;
import io.helidon.microprofile.grpc.core.GrpcMarshaller;
import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.core.Unary;

import com.oracle.coherence.helidon.io.PofMarshaller;
import com.oracle.io.pof.PortableTypeSerializer;
import com.oracle.io.pof.SimplePofContext;
import com.oracle.io.pof.annotation.PortableList;
import com.oracle.io.pof.annotation.PortableType;
import io.grpc.MethodDescriptor;

@RpcService
@GrpcMarshaller("carts")
public interface CartService {
    @Unary
    CompletableFuture<Cart> getCart(String cartId);

    // ---- inner class: Cart -----------------------------------------------

    @PortableType(id = 2)
    class Cart {
        @PortableList(elementClass = Item.class)
        public List<Item> items = new ArrayList<>();

        @Override
        public String toString() {
            return "Cart{" +
                    "items=" + items +
                    '}';
        }
    }
    // ---- inner class: Marshaller -----------------------------------------

    @ApplicationScoped
    @Named("carts")
    class Marshaller implements MarshallerSupplier {

        private final MethodDescriptor.Marshaller<?> marshaller;

        @SuppressWarnings("Duplicates")
        public Marshaller() {
            SimplePofContext ctx = new SimplePofContext()
                    .registerPortableTypes(Item.class, Cart.class);
            marshaller = new PofMarshaller(ctx);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> aClass) {
            return (MethodDescriptor.Marshaller<T>) marshaller;
        }
    }
}
