package com.fooddeliveryapp.models.users;

import com.fooddeliveryapp.models.notification.AppNotification;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String email;
    protected String phone;
    protected String password;
    protected List<AppNotification> notifications = new ArrayList<>();

    protected User(String name, String email, String phone, String password) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name required");

        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email required");

        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password required");

        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    /**
     * JDBC reconstruction constructor.
     * Restores a User from the database using its persisted {@code id}.
     */
    protected User(String id, String name, String email, String phone, String password) {
        this(name, email, phone, password);
        this.id = id;  // override the generated UUID with the stored one
    }

    /**
     * Restores the notification list from the database.
     * Called by JDBC repositories after object construction.
     */
    public void restoreNotifications(List<AppNotification> loaded) {
        this.notifications = new ArrayList<>(loaded);
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public abstract Role getRole();

    public void addNotification(String message) {
        notifications.add(new AppNotification(message));
    }

    public List<AppNotification> getNotifications() {
        return notifications;
    }

    public void removeNotification(String id) {
        notifications.removeIf(n -> n.getId().equals(id));
    }

    public void clearNotifications() {
        notifications.clear();
    }
}
