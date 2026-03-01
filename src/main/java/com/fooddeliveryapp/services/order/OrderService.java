package com.fooddeliveryapp.services.order;

import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.order.*;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.notification.*;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.delivery.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.notification.NotificationService;
import com.fooddeliveryapp.services.payment.PaymentStrategy;

import java.util.*;

public class OrderService {

    private final Repository<Order> orderRepository;
    private final DiscountService discountService;
    private final Repository<User> userRepository;
    private final DeliveryAssignmentStrategy deliveryStrategy;
    private final Queue<Order> waitingOrders = new LinkedList<>();
    private final NotificationService notificationService;

    public OrderService(Repository<Order> orderRepository,
                        DiscountService discountService,
                        Repository<User> userRepository,
                        DeliveryAssignmentStrategy deliveryStrategy, NotificationService notificationService) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.userRepository = userRepository;
        this.deliveryStrategy = deliveryStrategy;
        this.notificationService = notificationService;
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
        attachObservers(order);

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

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Only PAID orders can be confirmed.");
        }

        // 1️⃣ Confirm order by admin
        order.confirmByAdmin();

        // 2️⃣ Get all delivery partners
        List<DeliveryPartner> partners =
                userRepository.findAll().stream()
                        .filter(u -> u instanceof DeliveryPartner)
                        .map(u -> (DeliveryPartner) u)
                        .toList();

        // 3️⃣ Case: No partners registered
        if (partners.isEmpty()) {
            waitingOrders.add(order);
            System.out.println("No delivery partners registered. Order added to queue.");
            orderRepository.save(order);
            return;
        }

        // 4️⃣ Try assigning partner
        DeliveryPartner selected =
                deliveryStrategy.assign(order, partners);

        // 5️⃣ Case: All partners busy
        if (selected == null) {
            waitingOrders.add(order);
            System.out.println("All partners busy. Order added to queue.");
            orderRepository.save(order);
            return;
        }

        // 6️⃣ Assign partner
        order.assignDeliveryPartner(selected.getId());
        selected.setAvailable(false);

        // Optional: Phone notification observer
        new PhoneNotification(selected.getPhone())
                .update(order, "New order assigned. Order ID: " + order.getId());
        // 8️⃣ Persist changes
        userRepository.save(selected);
        orderRepository.save(order);
    }
    // ----------------------------
    // Delivery Complete
    // ----------------------------

    public void deliverOrder(String orderId,
                             String partnerId) {

        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Order is not out for delivery.");
        }

        if (!partnerId.equals(order.getDeliveryPartnerId())) {
            throw new IllegalStateException("This partner is not assigned to the order.");
        }

        order.markDelivered();

        DeliveryPartner partner = userRepository.findById(partnerId)
                .filter(u -> u instanceof DeliveryPartner)
                .map(u -> (DeliveryPartner) u)
                .orElseThrow(() ->
                        new IllegalArgumentException("Delivery Partner not found"));

        partner.setAvailable(true);

        userRepository.save(partner);
        orderRepository.save(order);

        assignNextFromQueue(partner);
    }

    // ----------------------------
    // Queue Handling
    // ----------------------------

    private void assignNextFromQueue(DeliveryPartner partner) {

        if (waitingOrders.isEmpty()) {
            return;
        }

        Order next = waitingOrders.poll();

        if (next == null) {
            return;
        }

        // Safety: Only assign confirmed orders
        if (next.getStatus() != OrderStatus.CONFIRMED_BY_ADMIN) {
            return;
        }

        // Assign partner (this also sets status to OUT_FOR_DELIVERY)
        next.assignDeliveryPartner(partner.getId());

        partner.setAvailable(false);

        // Persist both
        orderRepository.save(next);
        userRepository.save(partner);

        System.out.println("Queued order auto-assigned: " + next.getId());
    }

    private void attachObservers(Order order) {

        User user = userRepository.findById(order.getCustomerId())
                .orElseThrow();

        if (user instanceof Customer customer) {

            Set<NotificationType> prefs =
                    customer.getNotificationPreferences();

            if (prefs.contains(NotificationType.EMAIL)) {
                order.addObserver(
                        new EmailNotification(customer.getEmail()));
            }

            if (prefs.contains(NotificationType.PHONE)) {
                order.addObserver(
                        new PhoneNotification(customer.getPhone()));
            }

            // Always persist in-app notification
            order.addObserver(
                    new PersistentNotification(notificationService));
        }
    }

    private Customer findCustomerFromOrder(Order order) {
        return userRepository.findAll()
                .stream()
                .filter(u -> u instanceof Customer)
                .map(u -> (Customer) u)
                .filter(c -> c.getId().equals(order.getCustomerId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found"));
    }

    // ----------------------------
    // Reporting
    // ----------------------------

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCustomerId().equals(customerId))
                .filter(o -> o.getStatus() != OrderStatus.CREATED)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
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
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID
                        || o.getStatus() == OrderStatus.CONFIRMED_BY_ADMIN
                        || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY
                        || o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getFinalAmount)
                .sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public void cancelOrderByAdmin(
            String orderId) {

        Order order = findOrder(orderId);
        if(order.getStatus() != OrderStatus.ASSIGNED){
            throw new UnsupportedOperationException("Not able to cancle");
        }
        order.cancel();
        orderRepository.save(order);
    }

    private Order findOrder(String id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        return order;
    }

    public void tryAssignWaitingOrdersToPartner(DeliveryPartner partner) {

        if (!partner.isAvailable()) {
            return;
        }

        assignNextFromQueue(partner);
    }
}