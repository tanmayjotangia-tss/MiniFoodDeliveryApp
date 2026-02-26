package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.order.*;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.notification.*;
import com.fooddeliveryapp.services.DeliveryAssignment.DeliveryAssignmentStrategy;
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

        Order order = new Order(
                cart.getCustomer().getId(),
                cart.getCustomer().getName()
        );

        // 🔔 Attach Admin observer (console)
        order.addObserver(new ConsoleNotification());

        // 🔔 Attach Customer observer
        Customer customer = cart.getCustomer();

        if (customer.getNotificationPreference().equals("EMAIL")) {
            order.addObserver(
                    new EmailNotification(customer.getEmail()));
        } else {
            order.addObserver(
                    new PhoneNotification(customer.getPhone()));
        }

        for (CartItem ci : cart.getItems()) {
            order.addItem(new OrderItem(ci.getItem(),
                    ci.getQuantity()));
        }

        double discount =
                discountService.calculateDiscount(
                        order.getTotalAmount());

        order.applyDiscount(discount);

        paymentStrategy.pay(order.getFinalAmount());
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

            // 🔔 Attach delivery partner observer (phone)
            order.addObserver(
                    new PhoneNotification(selected.getPhone()));

            order.assignDeliveryPartner(selected.getId());
            selected.setAvailable(false);

        } catch (Exception e) {

            waitingOrders.add(order);
            System.out.println("All partners busy. Added to queue.");
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

    private void assignNextFromQueue(DeliveryPartner partner) {

        if (waitingOrders.isEmpty()) return;

        Order next = waitingOrders.poll();

        next.addObserver(
                new PhoneNotification(partner.getPhone()));

        next.assignDeliveryPartner(partner.getId());
        partner.setAvailable(false);

        orderRepository.save(next);
    }

    // --------------------------------------------------
// Reporting Methods (Required by AdminController)
// --------------------------------------------------

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCustomerId().equals(customerId))
                .toList();
    }

    public List<Order> getOrdersByPartner(String partnerId) {
        return orderRepository.findAll().stream()
                .filter(o -> partnerId.equals(o.getDeliveryPartnerId()))
                .toList();
    }

    public double calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getFinalAmount)
                .sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public void cancelOrderByAdmin(String orderId) {

        Order order = findOrder(orderId);

        order.cancel();

        orderRepository.save(order);
    }

    // ----------------------------
    private Order findOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found"));
    }
}