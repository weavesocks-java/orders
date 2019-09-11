package com.oracle.coherence.weavesocks.order;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 1)
public class PaymentRequest {
    @Portable private String orderId;
    @Portable private Address address;
    @Portable private Card card;
    @Portable private Customer customer;
    @Portable private float amount;

    public PaymentRequest() {
    }

    public PaymentRequest(String orderId, Address address, Card card, Customer customer, float amount) {
        this.orderId = orderId;
        this.address = address;
        this.customer = customer;
        this.card = card;
        this.amount = amount;
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
