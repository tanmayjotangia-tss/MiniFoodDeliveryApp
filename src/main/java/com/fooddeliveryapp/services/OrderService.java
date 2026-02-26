package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.services.DeliveryAssignment.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.payment.PaymentStrategy;

import java.util.List;

public class OrderService {

    private final Repository<Order> orderRepository;
    private final DiscountService discountService;
    private final Repository<DeliveryPartner> partnerRepository;
    private final DeliveryAssignmentStrategy deliveryStrategy;

    public OrderService(Repository<Order> orderRepository,
                        DiscountService discountService,
                        Repository<DeliveryPartner> partnerRepository,
                        DeliveryAssignmentStrategy deliveryStrategy) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.partnerRepository = partnerRepository;
        this.deliveryStrategy = deliveryStrategy;
    }

    // --------------------------------------------------
    // Checkout
    // --------------------------------------------------
    public Order checkoutCart(Cart cart,
                              PaymentStrategy paymentStrategy,
                              PaymentMode mode) {

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order(cart.getCustomer().getId());

        // Convert cart items to order items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem =
                    new OrderItem(cartItem.getItem(), cartItem.getQuantity());
            order.addItem(orderItem);
        }

        // Apply discount
        double discount = discountService
                .getCurrentStrategy()
                .calculate(order.getTotalAmount());

        order.applyDiscount(discount);

        // Process payment
        paymentStrategy.pay(order.getTotalAmount());

        // Mark paid
        order.markPaid(mode);

        orderRepository.save(order);
        cart.clearCart();

        return order;
    }

    // --------------------------------------------------
    // Admin Operations
    // --------------------------------------------------
    public void confirmOrder(String orderId) {

        Order order = findOrder(orderId);

        order.confirmByAdmin();

        orderRepository.save(order);
    }

    public void cancelOrderByAdmin(String orderId) {

        Order order = findOrder(orderId);

        order.cancel();

        orderRepository.save(order);
    }

    public void assignPartner(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        DeliveryPartner partner = partnerRepository
                .findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found"));

        // FIXED (previous bug removed)
        order.assignDeliveryPartner(partner.getId());

        orderRepository.save(order);
    }

    // --------------------------------------------------
    // Delivery Partner Operations
    // --------------------------------------------------
    public void acceptOrder(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        if (order.getDeliveryPartnerId() == null ||
                !order.getDeliveryPartnerId().equals(partnerId)) {

            throw new IllegalStateException("You are not assigned to this order.");
        }

        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Order is not ready to be accepted.");
        }

        order.markOutForDelivery();

        orderRepository.save(order);
    }

    public void deliverOrder(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        if (order.getDeliveryPartnerId() == null ||
                !order.getDeliveryPartnerId().equals(partnerId)) {

            throw new IllegalStateException("You are not assigned to this order.");
        }

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Order is not out for delivery.");
        }

        order.markDelivered();

        orderRepository.save(order);
    }

    // --------------------------------------------------
    // Reporting
    // --------------------------------------------------
    public double calculateTotalRevenue() {

        return orderRepository.findAll()
                .stream()
                .filter(order -> order.getStatus() != OrderStatus.CREATED)
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerId) {

        return orderRepository.findAll()
                .stream()
                .filter(o -> o.getCustomerId().equals(customerId))
                .toList();
    }

    public List<Order> getOrdersByPartner(String partnerId) {

        return orderRepository.findAll()
                .stream()
                .filter(o -> partnerId.equals(o.getDeliveryPartnerId()))
                .toList();
    }

    // --------------------------------------------------
    // Internal Helper
    // --------------------------------------------------
    private Order findOrder(String orderId) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID required");
        }

        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found: " + orderId));
    }
}