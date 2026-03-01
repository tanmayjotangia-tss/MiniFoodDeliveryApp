package com.fooddeliveryapp.models.notification;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.services.notification.NotificationService;
import com.fooddeliveryapp.services.notification.OrderObserver;

public class PersistentNotification implements OrderObserver {

    private final NotificationService notificationService;
    private final String targetUserId;

    public PersistentNotification(NotificationService service, String userId) {
        this.notificationService = service;
        this.targetUserId = userId;
    }

    @Override
    public void update(Order order, String message) {
        notificationService.notifyUser(targetUserId, message);
    }
}