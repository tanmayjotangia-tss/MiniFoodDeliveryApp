package com.fooddeliveryapp.models.notification;

import java.io.Serializable;

public class EmailNotification implements Observer, Serializable {

    private final String email;

    public EmailNotification(String email) {
        this.email = email;
    }

    @Override
    public void update(String message) {
        System.out.println("📧 Email sent to " + email + ": " + message);
    }
}
