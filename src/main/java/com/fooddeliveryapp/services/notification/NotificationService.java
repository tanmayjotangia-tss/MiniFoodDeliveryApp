package com.fooddeliveryapp.services.notification;

import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.User;

public class NotificationService {

    private final Repository<User> userRepository;

    public NotificationService(Repository<User> userRepository) {
        this.userRepository = userRepository;
    }

    public void notifyUser(String userId, String message) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.addNotification(message);

        userRepository.save(user);
    }
}