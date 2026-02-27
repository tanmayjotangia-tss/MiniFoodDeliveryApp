package com.fooddeliveryapp.services.notification;

import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.notification.NotificationType;

public class NotificationService {

    public void notifyCustomer(Customer customer, String message) {

        if (customer.getNotificationPreferences()
                .contains(NotificationType.EMAIL)) {

            sendEmail(customer.getEmail(), message);
        }

        if (customer.getNotificationPreferences()
                .contains(NotificationType.PHONE)) {

            sendSMS(customer.getPhone(), message);
        }
    }

    private void sendEmail(String email, String message) {
        System.out.println("Sending EMAIL to " + email + ": " + message);
    }

    private void sendSMS(String phone, String message) {
        System.out.println("Sending SMS to " + phone + ": " + message);
    }
}