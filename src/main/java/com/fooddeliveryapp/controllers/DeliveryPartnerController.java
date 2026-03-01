package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.delivery.DeliveryPartnerService;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;

public class DeliveryPartnerController {

    private final OrderService orderService;
    private final DeliveryPartnerService partnerService;
    private final AuthService authService;

    private DeliveryPartner loggedInPartner;

    public DeliveryPartnerController(OrderService orderService, DeliveryPartnerService partnerService, AuthService authService) {

        this.orderService = orderService;
        this.partnerService = partnerService;
        this.authService = authService;
    }

    public void start() {

        boolean running = true;

        while (running) {

            if (loggedInPartner == null) {
                running = showPreLoginMenu();
            } else {
                running = showDashboardMenu();
            }
        }
    }

    private boolean validateSession() {
        try {
            partnerService.findById(loggedInPartner.getId());
            return true;
        } catch (Exception e) {
            System.out.println("Your account has been removed by admin.");
            logout();
            return false;
        }
    }

    private boolean showPreLoginMenu() {

        System.out.println("\n=== DELIVERY PARTNER PANEL ===");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> handleLogin();

            case 2 -> handleRegister();

            case 3 -> {
                return false;
            }

            default -> System.out.println("Invalid choice.");
        }
        return true;
    }

    private boolean showDashboardMenu() {

        if (!validateSession()) {
            return true;
        }

        System.out.println("\n=== DELIVERY DASHBOARD ===");
        System.out.println("1. View Assigned Orders");
        System.out.println("2. Mark Order Delivered");
        System.out.println("3. View Order History");
        System.out.println("4. View Earning");
        System.out.println("5. Logout");
        System.out.println("6. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> viewOrders();

            case 2 -> deliverOrder();

            case 3 -> viewDeliveryHistory();

            case 4 -> viewEarning();

            case 5 -> logout();

            case 6 -> {
                return false;
            }

            default -> System.out.println("Invalid choice.");
        }

        return true;
    }

    //    Assigned Orders
    private void viewOrders() {

        try {
            List<Order> orders = orderService.getOrdersByPartner(loggedInPartner.getId())
                    .stream()
                    .filter(o -> o.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                    .toList();

            if (orders.isEmpty()) {
                System.out.println("No assigned orders.");
                return;
            }

            System.out.println("\nAssigned Orders:");

            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                System.out.println((i + 1) + ". ID: " + o.getId() + " | Customer: " + o.getCustomerName() + " | Status: " + o.getStatus() + " | Amount: ₹" + o.getTotalAmount());
            }
        } catch (Exception e) {
            System.out.println("Failed to load orders: " + e.getMessage());
        }
    }

    //    Mark Delivered
    private void deliverOrder() {

        List<Order> orders = orderService.getOrdersByPartner(loggedInPartner.getId())
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.OUT_FOR_DELIVERY).toList();

        if (orders.isEmpty()) {
            System.out.println("No orders ready.");
            return;
        }

        Order selected = selectOrder(orders);
        if (selected == null) return;

        try {
            orderService.deliverOrder(selected.getId(), loggedInPartner.getId());
            System.out.println("Order delivered successfully.");
        } catch (InvalidOperationException e) {
            System.out.println("Failed to mark order as delivered: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Failed to mark order as delivered: " + e.getMessage());
        }
    }

    //    Delivered History
    private void viewDeliveryHistory() {

        try {
            List<Order> delivered = orderService.getOrdersByPartner(loggedInPartner.getId())
                    .stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED).toList();

            if (delivered.isEmpty()) {
                System.out.println("No delivered orders yet.");
                return;
            }

            System.out.println("\n=== DELIVERY HISTORY ===");

            for (int i = 0; i < delivered.size(); i++) {
                Order o = delivered.get(i);
                System.out.println((i + 1) + ". ID: " + o.getId() + " | Customer: " + o.getCustomerName() + " | Amount: ₹" + o.getTotalAmount());
            }
        } catch (Exception e) {
            System.out.println("Failed to load delivery history: " + e.getMessage());
        }
    }

    private void viewEarning() {
        try {
            double totalEarnings = partnerService.calculateEarnings(loggedInPartner.getId());

            System.out.println("\n=== EARNINGS SUMMARY ===");
            System.out.println("Basic Pay: ₹" + loggedInPartner.getBasicPay());
            System.out.println("Incentive Percentage: " + loggedInPartner.getIncentivePercentage() + "%");
            System.out.println("----------------------------");
            System.out.println("Total Earnings: ₹" + totalEarnings);
        } catch (Exception e) {
            System.out.println("Failed to load earnings: " + e.getMessage());
        }
    }

    //    Select Order - No manual UUID
    private Order selectOrder(List<Order> orders) {

        System.out.println("\nSelect Order:");

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println((i + 1) + ". " + o.getCustomerName() + " | ₹" + o.getTotalAmount() + " | " + o.getStatus());
        }

        int choice = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (choice == 0) return null;

        if (choice < 1 || choice > orders.size()) {
            System.out.println("Invalid selection.");
            return null;
        }

        return orders.get(choice - 1);
    }

    private void handleLogin() {

        String email = InputUtil.readEmail("Enter Email: ");
        String password = InputUtil.readPassword("Enter Password: ");

        try {
            User user = authService.login(email, password);

            if (user instanceof DeliveryPartner partner) {
                loggedInPartner = partner;
                System.out.println("Delivery Partner logged in successfully.");
            } else {
                System.out.println("Invalid credentials.");
            }

        } catch (Exception e) {
            System.out.println("Invalid credentials. " + e.getMessage());
        }
    }

    private void handleRegister() {

        String name = InputUtil.readValidName("Enter Name: ");
        String email = InputUtil.readEmail("Enter Email: ");
        String phone = InputUtil.readPhoneNumber("Enter Phone: ");
        String password = InputUtil.readPassword("Enter Password: ");

        try {
            boolean success = authService.registerDeliveryPartner(name, email, phone, password);

            if (success) {
                System.out.println("Registration successful!");
            } else {
                System.out.println("Email already exists.");
            }
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void logout() {

        loggedInPartner = null;
        System.out.println("Logged out successfully.");
    }
}