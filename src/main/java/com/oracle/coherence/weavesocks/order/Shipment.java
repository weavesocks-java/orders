package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;
import java.util.Objects;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 1)
public class Shipment implements Serializable {
    @Portable private String id;
    @Portable private String trackingNumber;

    public Shipment() {
    }

    public Shipment(String id) {
        this.id = id;
    }

    public Shipment(String id, String trackingNumber) {
        this.id = id;
        this.trackingNumber = trackingNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shipment shipment = (Shipment) o;
        return id.equals(shipment.id) &&
                trackingNumber.equals(shipment.trackingNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, trackingNumber);
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "id='" + id + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                '}';
    }
}
