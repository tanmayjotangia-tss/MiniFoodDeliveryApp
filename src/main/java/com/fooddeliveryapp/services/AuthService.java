package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.users.Admin;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.models.repository.Repository;

import java.util.Optional;

public class AuthService {

    private final Repository<User> userRepository;

    public AuthService(Repository<User> userRepository) {
        this.userRepository = userRepository;
    }

    // =========================
    // REGISTER
    // =========================
    public boolean register(String name,
                            String email,
                            String phone,
                            String password,
                            String role) {

        // Check unique email
        Optional<User> existingUser =
                userRepository.findAll()
                        .stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(email))
                        .findFirst();

        if (existingUser.isPresent()) {
            return false; // Email already exists
        }

        User newUser;

        switch (role.toLowerCase()) {
            case "admin":
                newUser = Admin.getInstance(name, email, phone, password);
                break;

            case "customer":
                newUser = new Customer(name, email, phone, password);
                break;

            case "delivery":
                newUser = new DeliveryPartner(name, email, phone, password, 0);
                break;

            default:
                throw new IllegalArgumentException("Invalid role.");
        }

        userRepository.save(newUser);
        return true;
    }

    // =========================
    // LOGIN
    // =========================
    public User login(String email, String password) {

        return userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }
}