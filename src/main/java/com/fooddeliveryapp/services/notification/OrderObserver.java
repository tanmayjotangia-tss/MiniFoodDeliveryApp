package com.fooddeliveryapp.services.notification;

import com.fooddeliveryapp.models.order.Order;

public interface OrderObserver {
    void update(Order order, String message);
}