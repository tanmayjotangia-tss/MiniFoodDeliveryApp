package com.fooddeliveryapp.services.notification;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.notification.AppNotification;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.repository.DBNotificationRepository;
import com.fooddeliveryapp.repository.Repository;

public class NotificationService {

    private final Repository<User> userRepository;
    private final DBNotificationRepository notificationRepository;

    public NotificationService(Repository<User> userRepository, DBNotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public void notifyUser(String userId, String message) {

        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        AppNotification notification = new AppNotification(message);
        notificationRepository.insert(userId,notification);
    }
}