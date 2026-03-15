package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.notification.AppNotification;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.delivery.DeliveryPartnerService;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.ArrayList;
import java.util.Comparator;
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
        System.out.println("5. View Notifications");
        System.out.println("6. Logout");
        System.out.println("7. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> viewOrders();

            case 2 -> deliverOrder();

            case 3 -> viewDeliveryHistory();

            case 4 -> viewEarning();

            case 5 -> viewNotifications();

            case 6 -> logout();

            case 7 -> {
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
        try {
            List<Order> orders = orderService.getOrdersByPartner(loggedInPartner.getId())
                    .stream()
                    .filter(o -> o.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                    .toList();

            if (orders.isEmpty()) {
                System.out.println("No orders ready.");
                return;
            }

            Order selected = selectOrder(orders);
            if (selected == null) return;

            orderService.deliverOrder(selected.getId(), loggedInPartner.getId());
            System.out.println("Order delivered successfully.");

        } catch (InvalidOperationException e) {
            System.out.println("Failed to mark order as delivered: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error while delivering order: " + e.getMessage());
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

    private void viewNotifications() {

        List<AppNotification> notifications = new ArrayList<>(loggedInPartner.getNotifications());

        if (notifications.isEmpty()) {
            System.out.println("No notifications.");
            return;
        }

        // Sort newest first
        notifications.sort(Comparator.comparing(AppNotification::getTimestamp).reversed());

        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) notifications.size() / pageSize);

        int currentPage = 1;

        while (true) {

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, notifications.size());

            System.out.println("\n=== YOUR NOTIFICATIONS ===");
            System.out.println("Page " + currentPage + " of " + totalPages);
            System.out.println("--------------------------------------------------");

            for (int i = start; i < end; i++) {
                System.out.println((i + 1) + ". " + notifications.get(i));
            }

            System.out.println("--------------------------------------------------");
            System.out.println("1. Mark as Read");
            System.out.println("2. Delete");
            System.out.println("3. Next Page");
            System.out.println("4. Previous Page");
            System.out.println("5. Clear All");
            System.out.println("6. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> {
                    int index = InputUtil.readInt("Select notification number: ");
                    if (index >= 1 && index <= notifications.size()) {
                        notifications.get(index - 1).markAsRead();
                        System.out.println("Marked as read.");
                    }
                }

                case 2 -> {
                    int index = InputUtil.readInt("Select notification number: ");
                    if (index >= 1 && index <= notifications.size()) {
                        String id = notifications.get(index - 1).getId();
                        loggedInPartner.removeNotification(id);
                        notifications.remove(index - 1);
                        System.out.println("Deleted.");
                    }
                }

                case 3 -> {
                    if (currentPage < totalPages) currentPage++;
                }

                case 4 -> {
                    if (currentPage > 1) currentPage--;
                }

                case 5 -> {
                    loggedInPartner.clearNotifications();
                    System.out.println("All notifications cleared.");
                    return;
                }

                case 6 -> {
                    return;
                }

                default -> System.out.println("Invalid option.");
            }
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