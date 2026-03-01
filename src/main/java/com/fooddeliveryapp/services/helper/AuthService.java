package com.fooddeliveryapp.services.helper;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.order.OrderService;

import java.util.Optional;
import java.util.Set;

public class AuthService {

    private final Repository<User> userRepository;
    private final OrderService orderService;

    public AuthService(Repository<User> userRepository, OrderService orderService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    public boolean registerCustomer(String name, String email, String phone, String address, String password, Set<NotificationType> preferences) {

        if (emailExists(email)) {
            return false;
        }

        if (phoneExists(phone)) {
            return false;
        }

        User customer = new Customer(name, email, phone, address, password, preferences);

        userRepository.save(customer);
        return true;
    }

    public boolean registerDeliveryPartner(String name, String email, String phone, String password) {

        if (emailExists(email)) {
            return false;
        }

        if (phoneExists(phone)) {
            return false;
        }

        DeliveryPartner partner = new DeliveryPartner(name, email, phone, password, 5000);

        userRepository.save(partner);
        orderService.tryAssignWaitingOrdersToPartner(partner);
        return true;
    }

    public User login(String email, String password) {

        User user = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail()
                        .equalsIgnoreCase(email) && u.getPassword().equals(password))
                .findFirst().orElse(null);

        if(user == null){
            throw new EntityNotFoundException("User not found");
        }

        return user;
    }

    private boolean emailExists(String email) {
        Optional<User> existingUser = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail()
                        .equalsIgnoreCase(email)).findFirst();

        return existingUser.isPresent();
    }

    private boolean phoneExists(String phone) {
        return userRepository.findAll()
                .stream()
                .anyMatch(u -> u.getPhone().equals(phone));
    }
}