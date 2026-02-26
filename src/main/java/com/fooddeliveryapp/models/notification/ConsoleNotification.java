package com.fooddeliveryapp.models.notification;

public class ConsoleNotification implements Observer {

    @Override
    public void update(String message) {
        System.out.println("🔔 ADMIN NOTIFICATION: " + message);
    }
}
