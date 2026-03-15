package com.fooddeliveryapp.models.users;

import com.fooddeliveryapp.models.notification.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class DeliveryPartner extends User {

    private boolean available;
    private double basicPay;
    private double incentivePercentage;
    private final List<AppNotification> notifications = new ArrayList<>();

    public DeliveryPartner(String name, String email, String phone, String password, double basicPay) {

        super(name, email, phone, password);
        this.available = true;
        this.basicPay = basicPay;
        this.incentivePercentage = 5;
    }

    public DeliveryPartner(String id, String name, String email, String phone, String password,
            double basicPay, boolean available, double incentivePercentage) {
        super(id, name, email, phone, password);
        this.basicPay = basicPay;
        this.available = available;
        this.incentivePercentage = incentivePercentage;
    }

    @Override
    public void restoreNotifications(List<AppNotification> loaded) {
        this.notifications.clear();
        this.notifications.addAll(loaded);
    }

    public double getBasicPay() {
        return basicPay;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public Role getRole() {
        return Role.DELIVERY_PARTNER;
    }

    public void updateBasicPay(double newPay) {
        this.basicPay = newPay;
    }

    public double getIncentivePercentage() {
        return incentivePercentage;
    }

    public void updateIncentivePercentage(double percentage) {
        if (percentage < 0)
            throw new IllegalArgumentException("Invalid percentage");
        this.incentivePercentage = percentage;
    }

    public List<AppNotification> getNotifications() {
        return notifications;
    }

    public void addNotification(AppNotification notification) {
        notifications.add(notification);
    }

    public void removeNotification(String id) {
        notifications.removeIf(n -> n.getId().equals(id));
    }
}