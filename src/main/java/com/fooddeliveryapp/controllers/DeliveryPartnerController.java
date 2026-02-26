package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.AuthService;
import com.fooddeliveryapp.services.DeliveryPartnerService;
import com.fooddeliveryapp.services.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;

public class DeliveryPartnerController {

    private final OrderService orderService;
    private final DeliveryPartnerService partnerService;
    private final AuthService authService;

    private DeliveryPartner loggedInPartner;

    public DeliveryPartnerController(
            OrderService orderService,
            DeliveryPartnerService partnerService,
            AuthService authService) {

        this.orderService = orderService;
        this.partnerService = partnerService;
        this.authService = authService;
    }

    public void start() {

        while (true) {

            System.out.println("\n====== DELIVERY PARTNER PANEL ======");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. View Assigned Orders");
            System.out.println("4. Mark Delivered");
            System.out.println("5. View Order History");
            System.out.println("6. Logout");
            System.out.println("7. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> login();
                case 2 -> register();
                case 3 -> requireLogin(this::viewOrders);
                case 4 -> requireLogin(this::deliverOrder);
                case 5 -> requireLogin(this::viewDeliveryHistory);
                case 6 -> logout();
                case 7 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // --------------------------------------------
    // VIEW ACTIVE ORDERS (ASSIGNED + OUT_FOR_DELIVERY)
    // --------------------------------------------
    private void viewOrders() {

        List<Order> orders = orderService
                .getOrdersByPartner(loggedInPartner.getId())
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.ASSIGNED
                        || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .toList();

        if (orders.isEmpty()) {
            System.out.println("No assigned orders.");
            return;
        }

        System.out.println("\nAssigned Orders:");

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println((i + 1) + ". ID: " + o.getId()
                    + " | Customer: " + o.getCustomerName()
                    + " | Status: " + o.getStatus()
                    + " | Amount: ₹" + o.getTotalAmount());
        }
    }

    // --------------------------------------------
    // MARK DELIVERED
    // --------------------------------------------
    private void deliverOrder() {

        List<Order> orders = orderService
                .getOrdersByPartner(loggedInPartner.getId())
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.ASSIGNED
                        || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .toList();

        if (orders.isEmpty()) {
            System.out.println("No orders ready.");
            return;
        }

        Order selected = selectOrder(orders);
        if (selected == null) return;

        orderService.deliverOrder(
                selected.getId(),
                loggedInPartner.getId()
        );

        System.out.println("Order delivered successfully.");
    }

    // --------------------------------------------
    // DELIVERY HISTORY
    // --------------------------------------------
    private void viewDeliveryHistory() {

        List<Order> delivered = orderService
                .getOrdersByPartner(loggedInPartner.getId())
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .toList();

        if (delivered.isEmpty()) {
            System.out.println("No delivered orders yet.");
            return;
        }

        System.out.println("\n=== DELIVERY HISTORY ===");

        for (int i = 0; i < delivered.size(); i++) {
            Order o = delivered.get(i);
            System.out.println((i + 1) + ". ID: " + o.getId()
                    + " | Customer: " + o.getCustomerName()
                    + " | Amount: ₹" + o.getTotalAmount());
        }
    }

    // --------------------------------------------
    // SELECT ORDER
    // --------------------------------------------
    private Order selectOrder(List<Order> orders) {

        System.out.println("\nSelect Order:");

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println((i + 1) + ". "
                    + o.getCustomerName()
                    + " | ₹" + o.getTotalAmount()
                    + " | " + o.getStatus());
        }

        int choice = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (choice == 0) return null;

        if (choice < 1 || choice > orders.size()) {
            System.out.println("Invalid selection.");
            return null;
        }

        return orders.get(choice - 1);
    }

    // --------------------------------------------
    // LOGIN
    // --------------------------------------------
    private void login() {

        String email = InputUtil.readString("Enter Email: ");
        String password = InputUtil.readString("Enter Password: ");

        User user = authService.login(email, password);

        if (user instanceof DeliveryPartner partner) {
            loggedInPartner = partner;
            System.out.println("Delivery Partner logged in successfully.");
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    // --------------------------------------------
    // REGISTER
    // --------------------------------------------
    private void register() {

        String name = InputUtil.readString("Enter Name: ");
        String email = InputUtil.readString("Enter Email: ");
        String phone = InputUtil.readString("Enter Phone: ");
        String password = InputUtil.readString("Enter Password: ");

        authService.register(name, email, phone, password, "DELIVERY");
    }

    // --------------------------------------------
    // LOGIN CHECK
    // --------------------------------------------
    private void requireLogin(Runnable action) {

        if (loggedInPartner == null) {
            System.out.println("Please login first.");
            return;
        }

        action.run();
    }

    // --------------------------------------------
    // LOGOUT
    // --------------------------------------------
    private void logout() {

        loggedInPartner = null;
        System.out.println("Logged out successfully.");
    }
}