package com.fooddeliveryapp.services.notification;

import com.fooddeliveryapp.models.notification.*;
import com.fooddeliveryapp.models.users.*;
import com.fooddeliveryapp.models.repository.Repository;

public class NotificationService {

    private final Repository<User> userRepository;

    public NotificationService(Repository<User> userRepository) {
        this.userRepository = userRepository;
    }

    // 🎯 MAIN ENTRY POINT
    public void notifyUser(String userId, String message) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        // 1️⃣ Add In-App Notification
        user.addNotification(message);

        // 2️⃣ External Notifications (Observer style simulation)
        sendExternalNotifications(user, message);
    }

    // 🔔 Handle EMAIL + SMS via Observer
    private void sendExternalNotifications(User user, String message) {

        if (user instanceof Customer customer) {

            if (customer.getNotificationPreferences()
                    .contains(NotificationType.EMAIL)) {
                new EmailNotification(customer.getEmail()).update(message);
            }

            if (customer.getNotificationPreferences()
                    .contains(NotificationType.PHONE)) {
                new PhoneNotification(customer.getPhone()).update(message);
            }

        } else if (user instanceof DeliveryPartner partner) {

            // Delivery partner usually gets SMS
            new PhoneNotification(partner.getPhone())
                    .update(message);
        }
    }
}