package com.oracle.coherence.weavesocks.order;

import javax.inject.Named;

import io.helidon.grpc.core.MarshallerSupplier;
import io.helidon.microprofile.grpc.core.GrpcMarshaller;
import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.core.Unary;

import com.oracle.coherence.helidon.io.PofMarshaller;

import com.oracle.io.pof.PortableTypeSerializer;
import com.oracle.io.pof.SimplePofContext;

import io.grpc.MethodDescriptor;

@RpcService
@GrpcMarshaller("shipping")
public interface ShippingService {
    @Unary
    Shipment ship(Shipment shipment);

    // ---- inner class: Marshaller -----------------------------------------

    @Named("shipping")
    class Marshaller implements MarshallerSupplier {

        private final MethodDescriptor.Marshaller<?> marshaller;

        public Marshaller() {
            SimplePofContext ctx = new SimplePofContext();
            ctx.registerUserType(1, Shipment.class, new PortableTypeSerializer(1, Shipment.class));

            marshaller = new PofMarshaller(ctx);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> aClass) {
            return (MethodDescriptor.Marshaller<T>) marshaller;
        }
    }
}