package com.fooddeliveryapp.models.notification;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class AppNotification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String message;
    private final LocalDateTime timestamp;
    private boolean read;

    /** Standard constructor — generates a fresh UUID and timestamps now. */
    public AppNotification(String message) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    /**
     * JDBC reconstruction constructor.
     * Restores a notification exactly as it was persisted
     * (preserves id, timestamp, and read state).
     */
    public AppNotification(String id, String message, LocalDateTime timestamp, boolean read) {
        this.id        = id;
        this.message   = message;
        this.timestamp = timestamp;
        this.read      = read;
    }

    public String getId() {
        return id;
    }

    /** Needed by JDBC repositories to persist the read/unread state. */
    public boolean isRead() {
        return read;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void markAsRead() {
        this.read = true;
    }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    @Override
    public String toString() {

        String status = read ? "[READ]" : "[NEW]";

        return status + " " + getFormattedTime() + " | " + message;
    }
}