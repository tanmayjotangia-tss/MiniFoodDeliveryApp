package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.DeliveryPartner;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.services.DeliveryAssignment.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.payment.PaymentStrategy;

import java.util.List;

public class OrderService {

    private final Repository<Order> orderRepository;
    private final DiscountService discountService;
    private final Repository<DeliveryPartner> partnerRepository;
    private final DeliveryAssignmentStrategy deliveryStrategy;

    public OrderService(Repository<Order> orderRepository, DiscountService discountService, Repository<DeliveryPartner> partnerRepository, DeliveryAssignmentStrategy deliveryStrategy) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.partnerRepository = partnerRepository;
        this.deliveryStrategy = deliveryStrategy;
    }

    public void createOrder(Order order) {
        orderRepository.save(order);
    }

    public Order checkoutCart(Cart cart, PaymentStrategy paymentStrategy, PaymentMode mode) {

        if (cart.getItems().isEmpty()) throw new IllegalStateException("Cart is empty");

        Order order = new Order(cart.getCustomer());

        for (CartItem cartItem : cart.getItems()) {

            OrderItem orderItem = new OrderItem(cartItem.getItem(), cartItem.getQuantity());

            order.addItem(orderItem);
        }

        double discount = discountService.getCurrentStrategy().calculate(order.getFinalAmount());

        order.applyDiscount(discount);

        paymentStrategy.pay(order.getFinalAmount());

        order.markPaid(mode);

        orderRepository.save(order);

        cart.clearCart();

        return order;
    }

    public double calculateTotalRevenue() {

        return orderRepository.findAll().stream().filter(order -> order.getStatus() != OrderStatus.CREATED).mapToDouble(Order::getFinalAmount).sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private Order findOrder(String orderId) {

        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("Order ID is required");

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findAll()
                .stream()
                .filter(o -> o.getCustomer().getId().equals(customerId))
                .toList();
    }

    public void confirmOrder(String orderId) {

        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.PAID)
            throw new IllegalStateException("Only PAID orders can be confirmed.");

        order.confirmByAdmin();

        orderRepository.save(order);
    }

    public void assignPartner(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED_BY_ADMIN)
            throw new IllegalStateException("Order must be confirmed before assigning partner.");

        DeliveryPartner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalStateException("Delivery partner not found."));

        order.assignDeliveryPartner(partner);

        orderRepository.save(order);
    }

    public List<Order> getOrdersByPartner(String partnerId) {

        return orderRepository.findAll()
                .stream()
                .filter(order -> order.getAssignedPartner() != null && order.getAssignedPartner().getId().equals(partnerId))
                .toList();
    }

    public void acceptOrder(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        if (order.getAssignedPartner() == null || !order.getAssignedPartner().getId().equals(partnerId)) {

            throw new IllegalStateException("You are not assigned to this order.");
        }

        if (order.getStatus() != OrderStatus.ASSIGNED)
            throw new IllegalStateException("Order is not ready to be accepted.");

        order.markOutForDelivery();

        orderRepository.save(order);
    }

    public void deliverOrder(String orderId, String partnerId) {

        Order order = findOrder(orderId);

        if (order.getAssignedPartner() == null || !order.getAssignedPartner().getId().equals(partnerId)) {

            throw new IllegalStateException("You are not assigned to this order.");
        }

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY)
            throw new IllegalStateException("Order is not out for delivery.");

        order.markDelivered();

        orderRepository.save(order);
    }

    public void cancelOrderByAdmin(String orderId) {

        Order order = findOrder(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED)
            throw new IllegalStateException("Delivered order cannot be cancelled.");

        order.cancel();

        orderRepository.save(order);
    }
}
