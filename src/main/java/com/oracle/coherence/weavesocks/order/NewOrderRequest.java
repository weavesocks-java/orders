package com.oracle.coherence.weavesocks.order;

import java.net.URI;

public class NewOrderRequest {

    public URI customer;
    public URI address;
    public URI card;
    public URI items;

    public String customerId() {
        return getIdFromUri(customer);
    }

    public String addressId() {
        return getIdFromUri(address);
    }

    public String cardId() {
        return getIdFromUri(card);
    }

    public String cartId() {
        return items.getPath().split("/")[1];
    }

    @Override
    public String toString() {
        return "NewOrderRequest{" +
                "customer=" + customer +
                ", address=" + address +
                ", card=" + card +
                ", items=" + items +
                '}';
    }

    private String getIdFromUri(URI uri) {
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}