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
@GrpcMarshaller("payment")
public interface PaymentService {
    @Unary
    PaymentResponse authorize(PaymentRequest request);

    // ---- inner class: Marshaller -----------------------------------------

    @Named("payment")
    class Marshaller implements MarshallerSupplier {

        private final MethodDescriptor.Marshaller<?> marshaller;

        @SuppressWarnings("Duplicates")
        public Marshaller() {
            SimplePofContext ctx = new SimplePofContext();
            ctx.registerUserType(1, PaymentRequest.class, new PortableTypeSerializer(1, PaymentRequest.class));
            ctx.registerUserType(2, PaymentResponse.class, new PortableTypeSerializer(2, PaymentResponse.class));
            ctx.registerUserType(3, Address.class, new PortableTypeSerializer(3, Address.class));
            ctx.registerUserType(4, Card.class, new PortableTypeSerializer(4, Card.class));
            ctx.registerUserType(5, Customer.class, new PortableTypeSerializer(5, Customer.class));

            marshaller = new PofMarshaller(ctx);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> aClass) {
            return (MethodDescriptor.Marshaller<T>) marshaller;
        }
    }
}
