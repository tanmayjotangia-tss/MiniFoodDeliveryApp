package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.Customer;
import com.fooddeliveryapp.models.DeliveryPartner;
import com.fooddeliveryapp.models.PaymentMode;

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

        if (customer == null)
            throw new IllegalArgumentException("Customer required");

        this.id = UUID.randomUUID().toString();
        this.customer = customer;
    }


    public void addItem(OrderItem item) {

        if (status != OrderStatus.CREATED)
            throw new InvalidOperationException(
                    "Cannot modify order after payment");

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
            throw new InvalidOperationException(
                    "Discount can only be applied before payment");

        if (discountAmount < 0)
            throw new IllegalArgumentException("Invalid discount");

        this.discount = discountAmount;
        recalculate();
    }


    public void markPaid(PaymentMode mode) {

        if (status != OrderStatus.CREATED)
            throw new InvalidOperationException(
                    "Order already processed");

        if (mode == null)
            throw new IllegalArgumentException("Payment mode required");

        this.paymentMode = mode;
        this.status = OrderStatus.PAID;
    }

    public void assignDeliveryPartner(DeliveryPartner partner) {

        if (status != OrderStatus.PAID)
            throw new InvalidOperationException(
                    "Order must be paid before assigning delivery");

        if (partner == null)
            throw new IllegalArgumentException("Partner required");

        if (!partner.isAvailable())
            throw new InvalidOperationException(
                    "Delivery partner not available");

        this.assignedPartner = partner;
        partner.markUnavailable();

        this.status = OrderStatus.ASSIGNED;
    }

    public void markDelivered() {

        if (status != OrderStatus.ASSIGNED)
            throw new InvalidOperationException(
                    "Order not assigned");

        this.status = OrderStatus.DELIVERED;

        if (assignedPartner != null)
            assignedPartner.markAvailable();
    }

    public String getId() { return id; }

    public Customer getCustomer() { return customer; }

    public OrderStatus getStatus() { return status; }

    public double getFinalAmount() { return finalAmount; }

    public PaymentMode getPaymentMode() { return paymentMode; }

    public DeliveryPartner getAssignedPartner() { return assignedPartner; }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}