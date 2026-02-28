package com.fooddeliveryapp.models.notification;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AppNotification implements Serializable {

    private final String message;
    private final LocalDateTime timestamp;

    public AppNotification(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timestamp + " - " + message;
    }
}
