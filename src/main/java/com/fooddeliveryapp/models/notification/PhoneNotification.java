package com.fooddeliveryapp.models.notification;

public class PhoneNotification implements Observer {

    private final String phone;

    public PhoneNotification(String phone) {
        this.phone = phone;
    }

    @Override
    public void update(String message) {
        System.out.println("📱 SMS sent to " + phone + ": " + message);
    }
}