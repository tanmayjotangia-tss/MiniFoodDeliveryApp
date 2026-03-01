package com.fooddeliveryapp.models.notification;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.services.notification.NotificationService;
import com.fooddeliveryapp.services.notification.OrderObserver;

public class PersistentNotification implements OrderObserver {

    private final NotificationService notificationService;

    public PersistentNotification(NotificationService service) {
        this.notificationService = service;
    }

    @Override
    public void update(Order order, String message) {
        notificationService.notifyUser(order.getCustomerId(), message);
    }
}