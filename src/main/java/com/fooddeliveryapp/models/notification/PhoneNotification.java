package com.fooddeliveryapp.models.notification;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.services.notification.OrderObserver;

import java.io.Serializable;

public class PhoneNotification implements OrderObserver, Serializable {

    private final String phone;

    public PhoneNotification(String phone) {
        this.phone = phone;
    }

    @Override
    public void update(Order order, String message) {
        System.out.println("📱 SMS sent to " + phone + ": " + message);
    }
}