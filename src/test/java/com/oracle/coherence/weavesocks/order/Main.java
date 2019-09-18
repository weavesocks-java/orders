package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Base;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main is responsible for ...
 *
 * @author hr  2019.09.17
 */
public class Main {

    public static void newOrder(UriInfo uriInfo, NewOrderRequest request) throws Exception {
        /*
        CompletableFuture<CartService.Cart> cart = cartService.getCart(request.cartId());
        CompletableFuture<CustomerResponse> customer = customerService.getCustomer(new CustomerRequest(request));

        CompletableFuture.allOf(customer).join();
        LOGGER.log(Level.INFO, "Customer: " + customer.get());
        LOGGER.log(Level.INFO, "Cart: " + cart.get());

        String orderId = createOrderId();

        Link link = Link.fromMethod(OrderResource.class, "getOrder")
                .baseUri("http://orders/orders/")
                .rel("self")
                .build(orderId);

        CustomerResponse customerResponse = customer.get();
         */

        Customer customer = new Customer();
        customer.firstName = "Harvey";
        customer.lastName = "Raja";
        customer.id = "9";

        Address address = new Address();
        address.city = "SF";
        address.street = "Alabama St";
        address.country = "USA";
        address.postcode = "94110";

        Card card = new Card();
        card.ccv = "123";
        card.expires = "10/19";
        card.longNum = "8327492379472398";

        List<Item> listItems = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            Item item = new Item();
            
            item.itemId = Integer.valueOf(Base.getRandom().nextInt(100) + 1).toString();
            item.quantity = 10;
            item.unitPrice = 21.0f;

            listItems.add(item);
        }

        Order order = new Order(
                "17",
                customer,
                address,
                card,
                listItems);

        CacheFactory.getCache("orders").put(order.getId(), order);
    }

    public static void main(String[] asArgs) throws Exception {
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        // hack until CDI is working
        Session session = Session.create();
        PaymentSubscriber  paymentSubscriber  = new PaymentSubscriber(session, CacheFactory.getCache("orders"), null);

        //Subscriber<Order> subscriber = ShipmentSubscriber.createSubscriber();

        ShipmentSubscriber shipmentSubscriber = new ShipmentSubscriber(session, CacheFactory.getCache("orders"), null);

        while (true) {
            System.out.println("press a key to submit an order");
            System.in.read();
            newOrder(null, null);
        }
    }

    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
}
