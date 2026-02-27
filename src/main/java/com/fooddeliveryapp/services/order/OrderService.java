package com.fooddeliveryapp.services.order;

import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.order.*;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.notification.*;
import com.fooddeliveryapp.services.delivery.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.payment.PaymentStrategy;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OrderService {

    private final Repository<Order> orderRepository;
    private final DiscountService discountService;
    private final Repository<DeliveryPartner> partnerRepository;
    private final DeliveryAssignmentStrategy deliveryStrategy;
    private final Queue<Order> waitingOrders = new LinkedList<>();

    public OrderService(Repository<Order> orderRepository,
                        DiscountService discountService,
                        Repository<DeliveryPartner> partnerRepository,
                        DeliveryAssignmentStrategy deliveryStrategy) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.partnerRepository = partnerRepository;
        this.deliveryStrategy = deliveryStrategy;
    }

    // ----------------------------
    // Checkout
    // ----------------------------

    public Order checkoutCart(Cart cart,
                              PaymentStrategy paymentStrategy,
                              PaymentMode mode) {

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Customer customer = cart.getCustomer();

        Order order = new Order(customer.getId(), customer.getName());

        cart.getItems().forEach(cartItem -> {

            OrderItem orderItem = new OrderItem(
                    cartItem.getItem(),
                    cartItem.getQuantity()
            );

            order.addItem(orderItem);
        });

        double total = order.getTotalAmount();
        double discount = discountService.calculateDiscount(total);
        order.applyDiscount(discount);

        order.addObserver(new ConsoleNotification());

        if (customer.getNotificationPreferences().contains(NotificationType.EMAIL)) {
            order.addObserver(new EmailNotification(customer.getEmail()));
        }

        if (customer.getNotificationPreferences().contains(NotificationType.PHONE)) {
            order.addObserver(new PhoneNotification(customer.getPhone()));
        }

        paymentStrategy.pay(order.getTotalAmount());

        order.markPaid(mode);

        orderRepository.save(order);

        cart.clearCart();

        return order;
    }

    // ----------------------------
    // Confirm & Assign
    // ----------------------------

    public void confirmOrder(String orderId) {

        Order order = findOrder(orderId);
        order.confirmByAdmin();

        List<DeliveryPartner> partners =
                partnerRepository.findAll();

        try {

            DeliveryPartner selected =
                    deliveryStrategy.assign(order, partners);

            order.assignDeliveryPartner(
                    selected.getId()
            );

            selected.setAvailable(false);

            order.addObserver(
                    new PhoneNotification(selected.getPhone())
            );

        } catch (Exception e) {

            waitingOrders.add(order);
            System.out.println(
                    "All partners busy. Added to queue."
            );
        }

        orderRepository.save(order);
    }

    // ----------------------------
    // Delivery Complete
    // ----------------------------

    public void deliverOrder(String orderId,
                             String partnerId) {

        Order order = findOrder(orderId);

        order.markDelivered();

        DeliveryPartner partner =
                partnerRepository.findById(partnerId)
                        .orElseThrow();

        partner.setAvailable(true);

        orderRepository.save(order);

        assignNextFromQueue(partner);
    }

    // ----------------------------
    // Queue Handling
    // ----------------------------

    private void assignNextFromQueue(
            DeliveryPartner partner) {

        if (waitingOrders.isEmpty()) return;

        Order next = waitingOrders.poll();

        next.assignDeliveryPartner(
                partner.getId()
        );

        partner.setAvailable(false);

        orderRepository.save(next);

        // 🔔 DELIVERY PARTNER NOTIFICATION
        new PhoneNotification(
                partner.getPhone()
        ).update(
                "You have been assigned Order ID: "
                        + next.getId()
        );

        // 🔔 CUSTOMER → OUT FOR DELIVERY
        notifyCustomer(
                next,
                "Your order is out for delivery!"
        );
    }

    // ----------------------------
    // CUSTOMER NOTIFICATION
    // ----------------------------

    private void notifyCustomer(Order order,
                                String message) {

        Customer customer =
                findCustomerFromOrder(order);

        if (customer.getNotificationPreferences()
                .contains(NotificationType.EMAIL)) {

            new EmailNotification(
                    customer.getEmail()
            ).update(message);
        }

        if (customer.getNotificationPreferences()
                .contains(NotificationType.PHONE)) {

            new PhoneNotification(
                    customer.getPhone()
            ).update(message);
        }
    }

    // You must implement this properly
    // based on how you fetch customer
    private Customer findCustomerFromOrder(
            Order order) {

        // Example:
        // return userRepository.findById(order.getCustomerId())

        throw new UnsupportedOperationException(
                "Implement customer lookup logic here"
        );
    }

    // ----------------------------
    // Reporting
    // ----------------------------

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(
            String customerId) {

        return orderRepository.findAll()
                .stream()
                .filter(o ->
                        o.getCustomerId()
                                .equals(customerId))
                .toList();
    }

    public List<Order> getOrdersByPartner(
            String partnerId) {

        return orderRepository.findAll()
                .stream()
                .filter(o ->
                        partnerId.equals(
                                o.getDeliveryPartnerId()
                        ))
                .toList();
    }

    public double calculateTotalRevenue() {

        return orderRepository.findAll()
                .stream()
                .filter(o ->
                        o.getStatus()
                                == OrderStatus.DELIVERED)
                .mapToDouble(Order::getFinalAmount)
                .sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public void cancelOrderByAdmin(
            String orderId) {

        Order order = findOrder(orderId);
        order.cancel();
        orderRepository.save(order);
    }

    private Order findOrder(String id) {

        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found"));
    }
}