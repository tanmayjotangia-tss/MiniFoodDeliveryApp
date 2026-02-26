package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.users.User;

public class AuthService {

    private final UserRepository repository;
    private User loggedInUser;

    public AuthService(UserRepository repository) {
        this.repository = repository;
    }

    public User login(String email, String password) {

        User user = repository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (!user.getPassword().equals(password))
            throw new IllegalArgumentException("Invalid password");

        loggedInUser = user;
        return user;
    }

    public void logout() {
        loggedInUser = null;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    // 🔥 Authorize using class type
    public void authorize(Class<? extends User> roleType) {

        if (loggedInUser == null ||
                !roleType.isInstance(loggedInUser)) {

            throw new SecurityException("Access denied.");
        }
    }
}