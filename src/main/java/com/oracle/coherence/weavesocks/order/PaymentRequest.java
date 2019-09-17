package com.oracle.coherence.weavesocks.order;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 1)
public class PaymentRequest {
    @Portable private String orderId;
    @Portable private Customer customer;
    @Portable private Address address;
    @Portable private Card card;
    @Portable private float amount;

    public PaymentRequest(Order order) {
        this.orderId = order.getId();
        this.customer = order.getCustomer();
        this.address = order.getAddress();
        this.card = order.getCard();
        this.amount = order.getTotal();
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "orderId=" + orderId +
                ", address=" + address +
                ", card=" + card +
                ", customer=" + customer +
                ", amount=" + amount +
                '}';
    }
}
