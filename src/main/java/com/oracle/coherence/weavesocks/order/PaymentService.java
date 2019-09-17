package com.oracle.coherence.weavesocks.order;

import javax.enterprise.context.ApplicationScoped;
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
    Payment authorize(PaymentRequest request);

    // ---- inner class: Marshaller -----------------------------------------

    @ApplicationScoped
    @Named("payment")
    class Marshaller implements MarshallerSupplier {

        private final MethodDescriptor.Marshaller<?> marshaller;

        @SuppressWarnings("Duplicates")
        public Marshaller() {
            SimplePofContext ctx = new SimplePofContext()
                    .registerPortableType(PaymentRequest.class)
                    .registerPortableType(Payment.class)
                    .registerPortableType(Address.class)
                    .registerPortableType(Card.class)
                    .registerPortableType(Customer.class);

            marshaller = new PofMarshaller(ctx);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> aClass) {
            return (MethodDescriptor.Marshaller<T>) marshaller;
        }
    }
}
