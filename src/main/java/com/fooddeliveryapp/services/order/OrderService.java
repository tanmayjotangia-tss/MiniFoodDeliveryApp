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
                        DeliveryAssignmentStrategy deliveryStrategy,
                        NotificationService notificationService) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.userRepository = userRepository;
        this.deliveryStrategy = deliveryStrategy;
        this.notificationService = notificationService;

        // Re-populate the in-memory queue from persisted storage so that
        // CONFIRMED_BY_ADMIN orders are not lost across application restarts.
        rehydrateWaitingQueue();
    }

    // ----------------------------
    // Queue Rehydration (startup)
    // ----------------------------

    /**
     * Scans the order repository for every order whose status is
     * CONFIRMED_BY_ADMIN (i.e., admin confirmed but no partner was available
     * at that time) and re-enqueues them in chronological order.
     *
     * This MUST be called once at construction time — after the repository
     * field is set — so the queue survives application restarts.
     */
    private void rehydrateWaitingQueue() {
        orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED_BY_ADMIN)
                .sorted(Comparator.comparing(Order::getCreatedAt))   // oldest first → fair FIFO
                .forEach(waitingOrders::add);

        if (!waitingOrders.isEmpty()) {
            System.out.println("[OrderService] Rehydrated " + waitingOrders.size()
                    + " order(s) into the waiting queue from previous session.");
        }
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

        // Re-attach observers: the `observers` list on Order is transient and is
        // reset to an empty list when the order is deserialized from disk.
        // Every code path that calls a lifecycle method must re-attach first.
        attachObservers(order);

        // 1️⃣ Confirm order — internally fires notifyObservers("Your order is confirmed...")
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

        // 6️⃣ Add delivery partner as observer BEFORE calling assignDeliveryPartner(),
        // so their phone notification fires through the same observer pipeline
        // instead of bypassing it via a direct .update() call.
        order.addObserver(new PhoneNotification(selected.getPhone()));

        // 7️⃣ Assign partner — internally fires notifyObservers("Your order is out for delivery.")
        // Customer observers (email/phone/persistent) and the partner phone observer all receive it.
        order.assignDeliveryPartner(selected.getId());
        selected.setAvailable(false);

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

        // Drain the queue, skipping any orders that were cancelled
        // while waiting (e.g., admin cancelled mid-queue).
        while (!waitingOrders.isEmpty()) {

            Order next = waitingOrders.poll();

            if (next == null) {
                break;
            }

            // Re-fetch from repository to get the latest persisted status.
            // The in-memory queue holds a snapshot; the real source of truth
            // is the repository (handles restarts and concurrent mutations).
            Order fresh = orderRepository.findById(next.getId()).orElse(null);

            if (fresh == null) {
                // Order was deleted externally — skip it.
                System.out.println("[OrderService] Queued order " + next.getId()
                        + " no longer exists — skipping.");
                continue;
            }

            if (fresh.getStatus() != OrderStatus.CONFIRMED_BY_ADMIN) {
                // Order was cancelled or already assigned by another path — skip it.
                System.out.println("[OrderService] Queued order " + fresh.getId()
                        + " has status " + fresh.getStatus() + " — skipping.");
                continue;
            }

            // Found a valid order — assign the partner and stop.
            fresh.assignDeliveryPartner(partner.getId());
            partner.setAvailable(false);

            orderRepository.save(fresh);
            userRepository.save(partner);

            System.out.println("[OrderService] Queued order auto-assigned: "
                    + fresh.getId() + " → partner: " + partner.getName());
            return;
        }
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

    // ----------------------------
    // Reporting
    // ----------------------------

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCustomerId().equals(customerId))
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
        // Count only DELIVERED orders — these are the only orders where money
        // was actually earned. Including PAID / CONFIRMED_BY_ADMIN / OUT_FOR_DELIVERY
        // inflates revenue because those orders can still be cancelled.
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

        // Re-attach observers so the customer receives the cancellation notification.
        attachObservers(order);

        OrderStatus status = order.getStatus();

        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Cannot cancel a delivered order.");
        }

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Order is already cancelled.");
        }

        if (status == OrderStatus.OUT_FOR_DELIVERY) {
            // Free the assigned partner before cancelling so they can accept new orders.
            String partnerId = order.getDeliveryPartnerId();
            if (partnerId != null) {
                userRepository.findById(partnerId)
                        .filter(u -> u instanceof DeliveryPartner)
                        .map(u -> (DeliveryPartner) u)
                        .ifPresent(p -> {
                            p.setAvailable(true);
                            userRepository.save(p);
                            tryAssignWaitingOrdersToPartner(p);

                        });
            }
        }
        // order.cancel() internally fires notifyObservers("Order has been cancelled.")
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