package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.db.DatabaseInitializer;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.users.Admin;
import com.fooddeliveryapp.models.users.Role;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.repository.*;
import com.fooddeliveryapp.services.delivery.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.delivery.DeliveryPartnerService;
import com.fooddeliveryapp.services.delivery.FirstAvailableDeliveryAssignment;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.discount.TieredPercentageDiscount;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.menu.MenuService;
import com.fooddeliveryapp.services.notification.NotificationService;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;


public class ApplicationController {

    private final AdminController adminController;
    private final CustomerController customerController;
    private final DeliveryPartnerController deliveryPartnerController;
    private final DiscountService discountService;
    private final OrderService orderService;

    private final Menu menu;

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CartRepository cartRepository;


    public ApplicationController() {

        DatabaseInitializer.initialize();

        Repository<Menu>  menuRepository  = new DBMenuRepository();
        Repository<Order> orderRepository = new DBOrderRepository();

        this.userRepository = new DBUserRepository();
        this.cartRepository = new DBCartRepository(this.userRepository);

        List<Menu> menus = menuRepository.findAll();
        if (menus.isEmpty()) {
            this.menu = new Menu("My Restaurant");
            menuRepository.save(this.menu);
        } else {
            this.menu = menus.get(0);
        }

        DeliveryAssignmentStrategy deliveryStrategy = new FirstAvailableDeliveryAssignment();

        DBNotificationRepository notificationRepository = new DBNotificationRepository();
        NotificationService notificationService = new NotificationService(userRepository,notificationRepository);

        MenuService menuService = new MenuService(menuRepository, this.menu);

        DBDiscountRepository discountRepository = new DBDiscountRepository();

        TieredPercentageDiscount tieredDiscount = discountRepository.load();

        this.discountService = new DiscountService(tieredDiscount);

        this.orderService = new OrderService(orderRepository, discountService, userRepository,
                deliveryStrategy, notificationService);

        this.authService = new AuthService(userRepository, orderService);

        DeliveryPartnerService deliveryService =
                new DeliveryPartnerService(userRepository, orderService);

        this.adminController = new AdminController(menuService, deliveryService, discountService,
                orderService, this.menu, authService, discountRepository, cartRepository);

        this.customerController = new CustomerController(orderService, this.menu, authService,
                cartRepository, discountService, this.userRepository, notificationRepository);

        this.deliveryPartnerController =
                new DeliveryPartnerController(orderService, deliveryService, authService);

        initializeAdminIfNotExists();
    }

    private void initializeAdminIfNotExists() {
        if (userRepository.findAll()
                .stream()
                .noneMatch(u -> u.getRole() == Role.ADMIN)) {

            User admin = new Admin("System Admin", "admin@restaurant.com",
                    "9999999999", "Admin@123");

            userRepository.save(admin);
            System.out.println("Default Admin created (email: admin@restaurant.com, password: Admin@123)");
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