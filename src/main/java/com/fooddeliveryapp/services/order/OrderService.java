package com.fooddeliveryapp.services.order;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.notification.EmailNotification;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.notification.PersistentNotification;
import com.fooddeliveryapp.models.notification.PhoneNotification;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
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

    public OrderService(Repository<Order> orderRepository, DiscountService discountService, Repository<User> userRepository, DeliveryAssignmentStrategy deliveryStrategy, NotificationService notificationService) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.userRepository = userRepository;
        this.deliveryStrategy = deliveryStrategy;
        this.notificationService = notificationService;

        // Re-populate the in-memory queue from persisted storage so that
        // CONFIRMED_BY_ADMIN orders are not lost across application restarts.
        rehydrateWaitingQueue();
    }

//    Scan order repo for every order with confirm bye admin status and re-enques it
    private void rehydrateWaitingQueue() {
        orderRepository.findAll().stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED_BY_ADMIN).sorted(Comparator.comparing(Order::getCreatedAt))   // oldest first → fair FIFO
                .forEach(waitingOrders::add);

        if (!waitingOrders.isEmpty()) {
            System.out.println("[OrderService] Rehydrated " + waitingOrders.size() + " order(s) into the waiting queue from previous session.");
        }
    }

    public Order checkoutCart(Cart cart, PaymentStrategy paymentStrategy, PaymentMode mode) {

        if (cart.getItems().isEmpty()) {
            throw new InvalidOperationException("Cannot place order with empty cart.");
        }

        Customer customer = cart.getCustomer();

        Order order = new Order(customer.getId(), customer.getName());

        attachObservers(order);

        cart.getItems().forEach(cartItem -> {

            OrderItem orderItem = new OrderItem(cartItem.getItem(), cartItem.getQuantity());

            order.addItem(orderItem);
        });

        double total = order.getTotalAmount();
        if(total <= 0){
            throw new InvalidOperationException("Cannot place order with negative total amount.");
        }

        double discount = discountService.calculateDiscount(total);
        if(discount < 0){
            throw new InvalidOperationException("Cannot place order with negative discount.");
        }

        order.applyDiscount(discount);

        paymentStrategy.pay(order.getFinalAmount());

        order.markPaid(mode);
        orderRepository.save(order);

        cart.clearCart();

        return order;
    }

    public void confirmOrder(String orderId) {

        Order order = findOrder(orderId);
        if(order == null) {
            throw new InvalidOperationException("Cannot find order with id " + orderId);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Only PAID orders can be confirmed.");
        }

        // Re-attach observers
        attachObservers(order);

        // Confirm order — internally fires notifyObservers("Your order is confirmed...")
        order.confirmByAdmin();

        // Get all delivery partners
        List<DeliveryPartner> partners = userRepository.findAll().stream().filter(u -> u instanceof DeliveryPartner).map(u -> (DeliveryPartner) u).toList();

        // Case: No partners registered
        if (partners.isEmpty()) {
            waitingOrders.add(order);
            System.out.println("No delivery partners registered. Order added to queue.");
            orderRepository.save(order);
            return;
        }

        //  Try assigning partner
        DeliveryPartner selected = deliveryStrategy.assign(order, partners);

        //  Case: All partners busy
        if (selected == null) {
            waitingOrders.add(order);
            System.out.println("All partners busy. Order added to queue.");
            orderRepository.save(order);
            return;
        }

        // 7️⃣ Assign partner — internally fires notifyObservers("Your order is out for delivery.")
        order.assignDeliveryPartner(selected.getId());
        attachObservers(order);
        selected.setAvailable(false);

        // 8️⃣ Persist changes
        userRepository.save(selected);
        orderRepository.save(order);
    }

    public void deliverOrder(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Order is not out for delivery.");
        }

        if (!partnerId.equals(order.getDeliveryPartnerId())) {
            throw new IllegalStateException("This partner is not assigned to the order.");
        }

        DeliveryPartner partner = userRepository.findById(partnerId)
                .filter(u -> u instanceof DeliveryPartner)
                .map(u -> (DeliveryPartner) u)
                .orElseThrow(() -> new IllegalArgumentException("Delivery Partner not found"));

        order.markDelivered();
        partner.setAvailable(true);

        userRepository.save(partner);
        orderRepository.save(order);

        assignNextFromQueue(partner);
    }

    private void assignNextFromQueue(DeliveryPartner partner) {

        // Drain the queue, skipping any orders that were cancled
        // while waiting (e.g., admin cancled mid-queue).
        while (!waitingOrders.isEmpty()) {

            Order next = waitingOrders.poll();

            if (next == null) {
                break;
            }

            // Re-fetch from repository to get the latest persisted status.
            Order fresh = orderRepository.findById(next.getId()).orElse(null);

            if (fresh == null) {
                // Order was deleted externally — skip it.
                System.out.println("[OrderService] Queued order " + next.getId() + " no longer exists — skipping.");
                continue;
            }

            if (fresh.getStatus() != OrderStatus.CONFIRMED_BY_ADMIN) {
                // Order was cancled or already assigned by another path — skip it.
                System.out.println("[OrderService] Queued order " + fresh.getId() + " has status " + fresh.getStatus() + " — skipping.");
                continue;
            }

            attachObservers(fresh);
            fresh.assignDeliveryPartner(partner.getId());
            partner.setAvailable(false);

            orderRepository.save(fresh);
            userRepository.save(partner);

            System.out.println("[OrderService] Queued order auto-assigned: " + fresh.getId() + " → partner: " + partner.getName());
            return;
        }
    }

    private void attachObservers(Order order) {

        order.clearObservers();

        User user = userRepository.findById(order.getCustomerId()).orElseThrow();

        if (user instanceof Customer customer) {

            Set<NotificationType> prefs = customer.getNotificationPreferences();

            if (prefs.contains(NotificationType.EMAIL)) {
                order.addObserver(new EmailNotification(customer.getEmail()));
            }

            if (prefs.contains(NotificationType.PHONE)) {
                order.addObserver(new PhoneNotification(customer.getPhone()));
            }

            // Always persist in-app notification
            order.addObserver( new PersistentNotification(notificationService, order.getCustomerId())
            );        }

        String partnerId = order.getDeliveryPartnerId();

        if (partnerId != null) {

            userRepository.findById(partnerId)
                    .filter(u -> u instanceof DeliveryPartner)
                    .map(u -> (DeliveryPartner) u)
                    .ifPresent(partner ->
                            order.addObserver(
                                    new PersistentNotification(notificationService, partner.getId())
                            )
                    );
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findAll()
                .stream()
                .filter(o -> o.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getCreatedAt)
                        .reversed())
                .toList();
    }

    public List<Order> getOrdersByPartner(String partnerId) {

        return orderRepository.findAll()
                .stream()
                .filter(o -> partnerId.equals(o.getDeliveryPartnerId()))
                .toList();
    }

    public double calculateTotalRevenue() {
        return orderRepository.findAll()
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getFinalAmount).sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public void cancelOrderByAdmin(String orderId) {

        Order order = findOrder(orderId);

        if(order == null){
            throw new EntityNotFoundException("Cannot find order with id " + orderId);
        }
        // Re-attach observers so the customer receives the cancellation notification.
        attachObservers(order);
        OrderStatus status = order.getStatus();


        if(status == null){
            throw new EntityNotFoundException("Cannot find order status with id " + orderId);
        }

        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a delivered order.");
        }

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled.");
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
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        return order;
    }

    public void tryAssignWaitingOrdersToPartner(DeliveryPartner partner) {

        if (!partner.isAvailable()) {
            return;
        }
        assignNextFromQueue(partner);
    }
}