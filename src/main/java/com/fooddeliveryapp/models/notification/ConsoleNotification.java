package com.fooddeliveryapp.models.notification;

import java.io.Serializable;

public class ConsoleNotification implements Observer, Serializable {

    @Override
    public void update(String message) {
        System.out.println("🔔 ADMIN NOTIFICATION: " + message);
    }
}
