package com.oracle.coherence.weavesocks.order;

import java.util.Collection;

import io.helidon.microprofile.grpc.core.Unary;

public interface ShippingServiceClient {
    @Unary
    public Shipment ship(Shipment shipment);

    @Unary
    public Collection<Shipment> getAllShipments();

    @Unary
    Shipment getShipmentById(String id);
}