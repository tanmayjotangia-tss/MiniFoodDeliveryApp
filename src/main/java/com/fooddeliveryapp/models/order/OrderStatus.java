package com.fooddeliveryapp.models.order;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CONFIRMED_BY_ADMIN,
    ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REASSIGNING
}
