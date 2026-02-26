package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.models.users.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String customerId;
    private User customer;
    private PaymentMode paymentMode;
    private String deliveryPartnerId;

    private final List<OrderItem> items;
    private final LocalDateTime createdAt;

    private OrderStatus status;
    private double discount;

    public Order(String customerId) {

        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("Customer required");

        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.CREATED;
    }

    public void addItem(OrderItem item) {

        if (status != OrderStatus.CREATED)
            throw new IllegalStateException("Cannot modify order after payment");

        if (item == null)
            throw new IllegalArgumentException("Item required");

        items.add(item);
    }

    public void markPaid(PaymentMode mode) {

        if (items.isEmpty())
            throw new IllegalStateException("Cannot pay for empty order");

        if (status != OrderStatus.CREATED)
            throw new IllegalStateException("Order already processed");

        status = OrderStatus.PAID;
    }

    public void assignDeliveryPartner(String partnerId) {

        if (status != OrderStatus.PAID)
            throw new IllegalStateException("Order must be paid first");

        if (partnerId == null || partnerId.isBlank())
            throw new IllegalArgumentException("Delivery partner required");

        this.deliveryPartnerId = partnerId;
        status = OrderStatus.OUT_FOR_DELIVERY;
    }

    public void markDelivered() {

        if (status != OrderStatus.OUT_FOR_DELIVERY)
            throw new IllegalStateException("Order not out for delivery");

        status = OrderStatus.DELIVERED;
    }

    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(OrderItem::subtotal)
                .sum();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getId() { return id; }

    public String getCustomerId() { return customerId; }

    public String getDeliveryPartnerId() { return deliveryPartnerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public User getCustomer() {
        return customer;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("\nOrder ID: ").append(id)
                .append("\nStatus: ").append(status)
                .append("\nTotal: ₹").append(getTotalAmount())
                .append("\nItems:\n");

        items.forEach(item -> sb.append(item).append("\n"));

        return sb.toString();
    }

    public void confirmByAdmin() {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("Only PAID orders can be confirmed.");
        }

        this.status = OrderStatus.CONFIRMED_BY_ADMIN;
    }

    public void cancel() {

        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Delivered order cannot be cancelled.");
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled.");
        }

        this.status = OrderStatus.CANCELLED;
    }

    public void markOutForDelivery() {
        this.status = OrderStatus.OUT_FOR_DELIVERY;
    }

    public void applyDiscount(double discount) {

        if (discount < 0) {
            throw new IllegalArgumentException("Discount cannot be negative.");
        }

        double currentTotal = getTotalAmount();

        if (discount > currentTotal) {
            throw new IllegalArgumentException("Discount cannot exceed order total.");
        }

        this.discount = discount;
    }

    public double getFinalAmount() {
        return getTotalAmount() - discount;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }
}