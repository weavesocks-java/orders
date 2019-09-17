package com.oracle.coherence.weavesocks.order;

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
import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

import io.grpc.MethodDescriptor;

@RpcService
@GrpcMarshaller("customer")
public interface CustomerService {
    @Unary
    CompletableFuture<CustomerResponse> getCustomer(CustomerRequest request);

    // ---- inner class: CustomerRequest ------------------------------------

    @PortableType(id = 1)
    class CustomerRequest {
        @Portable String customerId;
        @Portable String addressId;
        @Portable String cardId;

        CustomerRequest(NewOrderRequest request) {
            this.customerId = request.customerId();
            this.addressId = request.addressId();
            this.cardId = request.cardId();
        }
    }

    // ---- inner class: CustomerResponse ------------------------------------

    @PortableType(id = 2)
    class CustomerResponse {
        @Portable Customer customer;
        @Portable Address address;
        @Portable Card card;

        @Override
        public String toString() {
            return "CustomerResponse{" +
                    "customer=" + customer +
                    ", address=" + address +
                    ", card=" + card +
                    '}';
        }
    }

    // ---- inner class: Marshaller -----------------------------------------

    @ApplicationScoped
    @Named("customer")
    class Marshaller implements MarshallerSupplier {

        private final MethodDescriptor.Marshaller<?> marshaller;

        @SuppressWarnings("Duplicates")
        public Marshaller() {
            SimplePofContext ctx = new SimplePofContext()
                    .registerPortableType(CustomerRequest.class)
                    .registerPortableType(CustomerResponse.class)
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
