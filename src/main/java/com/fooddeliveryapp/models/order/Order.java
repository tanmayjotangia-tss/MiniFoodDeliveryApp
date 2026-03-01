package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.models.notification.*;
import com.fooddeliveryapp.services.notification.OrderObserver;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String customerId;
    private final String customerName;

    private PaymentMode paymentMode;
    private String deliveryPartnerId;

    private final List<OrderItem> items;
    private final LocalDateTime createdAt;

    private OrderStatus status;
    private double discount;

    private transient List<OrderObserver> observers = new ArrayList<>();

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


//    Observer Methods
public void addObserver(OrderObserver observer) {
    if (observers == null) {
        observers = new ArrayList<>();
    }
    observers.add(observer);
}


    public void notifyObservers(String message) {
        if (observers != null) {
            for (OrderObserver observer : observers) {
                observer.update(this, message);
            }
        }
    }

//    Order Lifecycle
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

        notifyObservers("Order placed successfully. Order ID: " + id);
    }

    public void confirmByAdmin() {

        if (status != OrderStatus.PAID)
            throw new IllegalStateException("Only PAID orders allowed");

        status = OrderStatus.CONFIRMED_BY_ADMIN;
    }

    public void assignDeliveryPartner(String partnerId) {

        if (status != OrderStatus.CONFIRMED_BY_ADMIN)
            throw new IllegalStateException("Order not confirmed");

        this.deliveryPartnerId = partnerId;
        status = OrderStatus.OUT_FOR_DELIVERY;

        notifyObservers("Your order is out for delivery.");
    }

    public void markDelivered() {

        if (status != OrderStatus.OUT_FOR_DELIVERY)
            throw new IllegalStateException("Not out for delivery");

        status = OrderStatus.DELIVERED;

        notifyObservers("Your order has been delivered.");

    }

    public void applyDiscount(double discount) {
        this.discount = discount;
    }

    public void cancel() {

        if (status == OrderStatus.DELIVERED)
            throw new IllegalStateException(
                    "Delivered order cannot be cancelled."
            );

        if (status == OrderStatus.CANCELLED)
            throw new IllegalStateException(
                    "Order already cancelled."
            );

        this.status = OrderStatus.CANCELLED;

        notifyObservers("Order has been cancelled.");
    }

    public String getId() { return id; }

    public String getCustomerId() { return customerId; }

    public String getCustomerName() { return customerName; }

    public String getDeliveryPartnerId() { return deliveryPartnerId; }

    public OrderStatus getStatus() { return status; }

    public PaymentMode getPaymentMode() { return paymentMode; }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(OrderItem::subtotal)
                .sum();
    }

    public double getFinalAmount() {
        return getTotalAmount() - discount;
    }

//    Serialization Safety
    private void readObject(java.io.ObjectInputStream in)
            throws Exception {

        in.defaultReadObject();
        observers = new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}