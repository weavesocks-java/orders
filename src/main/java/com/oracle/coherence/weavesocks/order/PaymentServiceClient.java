package com.oracle.coherence.weavesocks.order;

import io.helidon.microprofile.grpc.core.Unary;

public interface PaymentServiceClient {
    @Unary
    public PaymentResponse authorize(PaymentRequest request);
}
