package com.fooddeliveryapp.services.helper;

import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.models.repository.Repository;

import java.util.Optional;
import java.util.Set;

public class AuthService {

    private final Repository<User> userRepository;
    private final Repository<DeliveryPartner> partnerRepository;

    public AuthService(Repository<User> userRepository, Repository<DeliveryPartner> partnerRepository) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
    }

    // =========================
    // REGISTER CUSTOMER
    // =========================
    public boolean registerCustomer(String name,
                                    String email,
                                    String phone,
                                    String password,
                                    Set<NotificationType> preferences) {

        if (emailExists(email)) {
            return false;
        }

        User customer = new Customer(
                name,
                email,
                phone,
                password,
                preferences
        );
        userRepository.save(customer);
        return true;
    }

    // =========================
    // REGISTER DELIVERY PARTNER
    // =========================
    public boolean registerDeliveryPartner(String name,
                                           String email,
                                           String phone,
                                           String password) {

        if (emailExists(email)) {
            return false;
        }

        DeliveryPartner partner = new DeliveryPartner(
                name,
                email,
                phone,
                password,
                0
        );

        partnerRepository.save(partner);

        return true;
    }

    // =========================
    // LOGIN
    // =========================

    public User login(String email, String password) {

        // Check normal users
        User user = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);

        if (user != null) {
            return user;
        }

        // Check delivery partners
        return partnerRepository.findAll()
                .stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(email)
                        && p.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    // =========================
    // HELPER
    // =========================
    private boolean emailExists(String email) {
        Optional<User> existingUser = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();

        return existingUser.isPresent();
    }
}