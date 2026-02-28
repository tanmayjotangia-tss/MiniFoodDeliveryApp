package com.fooddeliveryapp.models.users;

import com.fooddeliveryapp.models.notification.NotificationType;

import java.io.Serializable;
import java.util.Set;

public class Customer extends User implements Serializable {
    private Set<NotificationType> notificationPreferences;

    public Customer(String name, String email, String phone, String password, Set<NotificationType> notificationPreferences) {

        super(name, email, phone, password);
        this.notificationPreferences = notificationPreferences;
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }

    public Set<NotificationType> getNotificationPreferences() {
        return notificationPreferences;
    }
}