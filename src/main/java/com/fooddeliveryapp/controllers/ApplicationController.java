package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.repository.*;
import com.fooddeliveryapp.models.users.*;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.services.delivery.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.delivery.DeliveryPartnerService;
import com.fooddeliveryapp.services.delivery.FirstAvailableDeliveryAssignment;
import com.fooddeliveryapp.services.discount.AmountDiscount;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.helper.FileRepository;
import com.fooddeliveryapp.services.menu.MenuService;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;


public class ApplicationController {

    private final AdminController adminController;
    private final CustomerController customerController;
    private final DeliveryPartnerController deliveryPartnerController;

    private final Menu menu;

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CartRepository cartRepository;

    public ApplicationController() {

        // Serialize Repositories
        Repository<Menu> menuRepository = new FileRepository<>("menu.dat");
        Repository<Order> orderRepository = new FileRepository<>("orders.dat");
        Repository<DeliveryPartner> partnerRepository = new FileRepository<>("partners.dat");

        this.userRepository = new FileUserRepository("users.dat");
        this.authService = new AuthService(userRepository);
        this.cartRepository = new FileCartRepository("carts.dat");

        // Load Menu
        List<Menu> menus = menuRepository.findAll();
        if (menus.isEmpty()) {
            this.menu = new Menu("My Restaurant");
            menuRepository.save(this.menu);
        } else {
            this.menu = menus.get(0);
        }

        // Default Discount
        DiscountService discountService = new DiscountService(new AmountDiscount(500, 10));

        DeliveryAssignmentStrategy deliveryStrategy = new FirstAvailableDeliveryAssignment();

        //Services
        MenuService menuService = new MenuService(menuRepository);

        OrderService orderService = new OrderService(orderRepository, discountService, partnerRepository, deliveryStrategy);

        DeliveryPartnerService deliveryService = new DeliveryPartnerService(partnerRepository, orderService);
        // Controllers
        this.adminController = new AdminController(menuService, deliveryService, discountService, orderService, this.menu, authService);

        this.customerController = new CustomerController(orderService, this.menu, authService, cartRepository);

        this.deliveryPartnerController = new DeliveryPartnerController(orderService, deliveryService, authService);

        initializeAdminIfNotExists();
    }

    // Ensure only one admin exists
    private void initializeAdminIfNotExists() {
        if (userRepository.findAll().stream().noneMatch(u -> u.getRole() == Role.ADMIN)) {

            User admin = new Admin("System Admin", "admin@restaurant.com", "9999999999", "admin123");

            userRepository.save(admin);
            System.out.println("Default Admin created (email: admin@restaurant.com, password: admin123)");
        }
    }

    public void start() {

        while (true) {

            System.out.println("\n=== RESTAURANT SYSTEM ===");
            System.out.println("1. Admin Panel");
            System.out.println("2. Customer Panel");
            System.out.println("3. Delivery Partner Panel");
            System.out.println("4. Exit");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> adminController.start();
                case 2 -> customerController.start();
                case 3 -> deliveryPartnerController.start();
                case 4 -> {
                    System.out.println("Exiting application...");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}