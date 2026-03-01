package com.fooddeliveryapp.models.notification;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.services.notification.OrderObserver;

import java.io.Serializable;

public class EmailNotification implements OrderObserver, Serializable {

    private final String email;

    public EmailNotification(String email) {
        this.email = email;
    }

    @Override
    public void update(Order order, String message) {
        System.out.println("📧 Email sent to " + email + ": " + message);
    }
}
