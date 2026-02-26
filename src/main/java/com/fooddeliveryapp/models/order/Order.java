package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.models.notification.Observer;
import com.fooddeliveryapp.models.users.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Order implements Serializable {

    private final String id;
    private final String customerId;
    private final String customerName;

    private PaymentMode paymentMode;
    private String deliveryPartnerId;

    private final List<OrderItem> items;
    private final LocalDateTime createdAt;

    private OrderStatus status;
    private double discount;

    // 🔔 Observer list
    private final List<Observer> observers = new ArrayList<>();

    public Order(String customerId, String customerName) {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("Customer required");

        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.CREATED;
    }

    // ----------------------------
    // Observer Methods
    // ----------------------------
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    private void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    // ----------------------------
    // Business Logic
    // ----------------------------
    public void addItem(OrderItem item) {
        if (status != OrderStatus.CREATED)
            throw new IllegalStateException("Cannot modify after payment");
        items.add(item);
    }

    public void markPaid(PaymentMode mode) {
        if (items.isEmpty())
            throw new IllegalStateException("Empty order");

        this.status = OrderStatus.PAID;
        this.paymentMode = mode;

        notifyObservers("New Order Placed by "
                + customerName + " | ₹" + getFinalAmount());
    }

    public void confirmByAdmin() {
        if (status != OrderStatus.PAID)
            throw new IllegalStateException("Only PAID orders allowed");
        status = OrderStatus.CONFIRMED_BY_ADMIN;
    }

    public void assignDeliveryPartner(String partnerId) {
        this.deliveryPartnerId = partnerId;
        status = OrderStatus.OUT_FOR_DELIVERY;

        notifyObservers("Order assigned to delivery partner.");
    }

    public void markDelivered() {
        if (status != OrderStatus.OUT_FOR_DELIVERY)
            throw new IllegalStateException("Not out for delivery");

        status = OrderStatus.DELIVERED;

        notifyObservers("Order delivered successfully.");
    }

    public void applyDiscount(double discount) {
        this.discount = discount;
    }

    // ----------------------------
    // Getters
    // ----------------------------
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getDeliveryPartnerId() { return deliveryPartnerId; }
    public OrderStatus getStatus() { return status; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getTotalAmount() {
        return items.stream().mapToDouble(OrderItem::subtotal).sum();
    }

    public double getFinalAmount() {
        return getTotalAmount() - discount;
    }

    public void cancel() {

        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Delivered order cannot be cancelled.");
        }

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order already cancelled.");
        }

        this.status = OrderStatus.CANCELLED;

        notifyObservers("Order has been cancelled.");
    }
}