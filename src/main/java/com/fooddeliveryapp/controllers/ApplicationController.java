package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.repository.FileUserRepository;
import com.fooddeliveryapp.models.repository.UserRepository;
import com.fooddeliveryapp.models.users.Admin;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.services.*;
import com.fooddeliveryapp.services.DeliveryAssignment.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.DeliveryAssignment.RandomDeliveryAssignment;
import com.fooddeliveryapp.services.discount.AmountDiscount;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;

public class ApplicationController {

    private final AdminController adminController;
    private final CustomerController customerController;
    private final DeliveryPartnerController deliveryPartnerController;

    private final Menu menu;

    private final UserRepository userRepository;
    private final AuthService authService;

    public ApplicationController() {

        // ---------------- Serialization Repositories ----------------
        Repository<Menu> menuRepository = new FileRepository<>("menu.dat");
        Repository<Order> orderRepository = new FileRepository<>("orders.dat");
        Repository<DeliveryPartner> partnerRepository = new FileRepository<>("partners.dat");

        this.userRepository = new FileUserRepository("users.dat");
        this.authService = new AuthService((Repository<User>) userRepository);

        // ---------------- Load Menu ----------------
        List<Menu> menus = menuRepository.findAll();
        if (menus.isEmpty()) {
            this.menu = new Menu("My Restaurant");
            menuRepository.save(this.menu);
        } else {
            this.menu = menus.get(0);
        }

        // ---------------- Default Discount ----------------
        DiscountService discountService =
                new DiscountService(new AmountDiscount(500, 10));

        DeliveryAssignmentStrategy deliveryStrategy =
                new RandomDeliveryAssignment();

        // ---------------- Services ----------------
        MenuService menuService = new MenuService(menuRepository);
        DeliveryPartnerService deliveryService =
                new DeliveryPartnerService(partnerRepository);

        OrderService orderService =
                new OrderService(
                        orderRepository,
                        discountService,
                        partnerRepository,
                        deliveryStrategy
                );

        // ---------------- Controllers ----------------
        this.adminController =
                new AdminController(
                        menuService,
                        deliveryService,
                        discountService,
                        orderService,
                        this.menu
                );

        this.customerController =
                new CustomerController(orderService, this.menu);

        this.deliveryPartnerController =
                new DeliveryPartnerController(orderService, deliveryService);

        initializeAdminIfNotExists();
    }

    // Ensure only one admin exists
    private void initializeAdminIfNotExists() {
        if (userRepository.findAll()
                .stream()
                .noneMatch(u -> "ADMIN".equals(u.getRole()))) {

            User admin = new Admin(
                    "System Admin",
                    "admin@restaurant.com",
                    "9999999999",
                    "admin123"
            );


            userRepository.save(admin);
            System.out.println("Default Admin created (email: admin@restaurant.com, password: admin123)");
        }
    }

    private boolean isMenuEmpty() {
        return menu.getRootCategory().getComponents().stream()
                .filter(c -> c instanceof MenuCategory)
                .map(c -> (MenuCategory) c)
                .allMatch(category -> category.getComponents().isEmpty());
    }

    public void start() {

        while (true) {

            System.out.println("\n=== RESTAURANT SYSTEM ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> handleLogin();

                case 2 -> handleRegistration();

                case 3 -> {
                    System.out.println("Exiting application...");
                    return;
                }

                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void handleLogin() {

        String email = InputUtil.readEmail("Enter Email: ");
        String password = InputUtil.readString("Enter Password: ");

        User user = authService.login(email, password);

        if (user == null) {
            System.out.println("Invalid credentials.");
            return;
        }

        System.out.println("Login successful!");

        switch (user.getRole()) {

            case "ADMIN" -> adminController.start();

            case "CUSTOMER" -> {
                if (isMenuEmpty()) {
                    System.out.println("Menu is empty. Contact admin.");
                } else {
                    customerController.start();
                }
            }

            case "DELIVERY" -> deliveryPartnerController.start();

            default -> System.out.println("Invalid role.");
        }
    }

    private void handleRegistration() {

        String name = InputUtil.readValidName("Enter Name: ");
        String email = InputUtil.readEmail("Enter Email: ");
        String phone = InputUtil.readString("Enter Phone: ");
        String password = InputUtil.readString("Enter Password: ");

        System.out.println("Select Role:");
        System.out.println("1. Customer");
        System.out.println("2. Delivery Partner");

        int roleChoice = InputUtil.readInt("Enter choice: ");

        String role;

        if (roleChoice == 1) {
            role = "CUSTOMER";
        } else if (roleChoice == 2) {
            role = "DELIVERY";
        } else {
            System.out.println("Invalid role.");
            return;
        }

        boolean success = authService.register(
                name,
                email,
                phone,
                password,
                role
        );

        if (success) {
            System.out.println("Registration successful!");
        } else {
            System.out.println("Email already exists.");
        }
    }
}