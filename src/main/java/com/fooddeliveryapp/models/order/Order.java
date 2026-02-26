package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;

import java.io.Serializable;
import java.util.*;

public class Order implements Serializable {

    private final String id;
    private final Customer customer;
    private final List<OrderItem> items = new ArrayList<>();

    private OrderStatus status = OrderStatus.CREATED;

    private double total;
    private double discount;
    private double finalAmount;

    private PaymentMode paymentMode;
    private DeliveryPartner assignedPartner;

    public Order(Customer customer) {

        if (customer == null) throw new IllegalArgumentException("Customer required");

        this.id = UUID.randomUUID().toString();
        this.customer = customer;
    }


    public void addItem(OrderItem item) {

        if (status != OrderStatus.CREATED) throw new InvalidOperationException("Cannot modify order after payment");

        items.add(item);
        recalculate();
    }

    private void recalculate() {

        total = items.stream()
                .mapToDouble(OrderItem::subtotal)
                .sum();

        finalAmount = total - discount;
    }

    public void applyDiscount(double discountAmount) {

        if (status != OrderStatus.CREATED)
            throw new InvalidOperationException("Discount can only be applied before payment");

        if (discountAmount < 0) throw new IllegalArgumentException("Invalid discount");

        this.discount = discountAmount;
        recalculate();
    }


    public void markPaid(PaymentMode mode) {

        if (status != OrderStatus.CREATED) throw new IllegalStateException("Invalid state transition.");

        this.paymentMode = mode;
        this.status = OrderStatus.PAID;
    }

    public void assignDeliveryPartner(DeliveryPartner partner) {

        if (this.status != OrderStatus.CONFIRMED_BY_ADMIN)
            throw new IllegalStateException("Order must be confirmed before assigning delivery");

        if (partner == null) throw new IllegalArgumentException("Partner required");

        if (!partner.isAvailable()) throw new InvalidOperationException("Delivery partner not available");

        this.assignedPartner = partner;
        partner.markUnavailable();

        this.status = OrderStatus.ASSIGNED;
    }

    public String getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public DeliveryPartner getAssignedPartner() {
        return assignedPartner;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void markPaymentPending() {
        if (status != OrderStatus.CREATED) throw new IllegalStateException("Invalid state transition");
        this.status = OrderStatus.PAYMENT_PENDING;
    }

    public void markPaid() {
        if (status != OrderStatus.PAYMENT_PENDING) throw new IllegalStateException("Payment not expected now");
        this.status = OrderStatus.PAID;
    }

    public void confirmByAdmin() {
        if (status != OrderStatus.PAID) throw new IllegalStateException("Order must be paid first");
        this.status = OrderStatus.CONFIRMED_BY_ADMIN;
    }

    public void markOutForDelivery() {
        if (status != OrderStatus.ASSIGNED) throw new IllegalStateException("Order not assigned yet");
        this.status = OrderStatus.OUT_FOR_DELIVERY;
    }

    public void markDelivered() {
        if (status != OrderStatus.OUT_FOR_DELIVERY) throw new IllegalStateException("Order not out for delivery");
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED) throw new IllegalStateException("Cannot cancel delivered order");
        this.status = OrderStatus.CANCELLED;
    }
}