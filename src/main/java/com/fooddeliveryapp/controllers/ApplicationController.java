package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.db.DatabaseInitializer;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.repository.*;
import com.fooddeliveryapp.models.users.Admin;
import com.fooddeliveryapp.models.users.Role;
import com.fooddeliveryapp.models.users.User;
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

        // ── Step 1: Initialise the database (create tables if needed) ─────────
        DatabaseInitializer.initialize();

        // ── Step 2: Wire JDBC repositories (replaces File* implementations) ───
        //
        //  OLD:  new FileRepository<>("menu.dat")
        //  NEW:  new DBMenuRepository()
        //
        //  OLD:  new FileRepository<>("orders.dat")
        //  NEW:  new DBOrderRepository()
        //
        //  OLD:  new FileUserRepository("users.dat")
        //  NEW:  new DBUserRepository()
        //
        //  OLD:  new FileCartRepository("carts.dat")
        //  NEW:  new DBCartRepository(userRepository)
        //
        //  OLD:  new FileDiscountRepository("discount.dat")
        //  NEW:  new DBDiscountRepository()

        Repository<Menu>  menuRepository  = new DBMenuRepository();
        Repository<Order> orderRepository = new DBOrderRepository();

        this.userRepository = new DBUserRepository();
        this.cartRepository = new DBCartRepository(this.userRepository);

        // ── Step 3: Load (or create) the single Menu ──────────────────────────
        List<Menu> menus = menuRepository.findAll();
        if (menus.isEmpty()) {
            this.menu = new Menu("My Restaurant");
            menuRepository.save(this.menu);
        } else {
            this.menu = menus.get(0);
        }

        // ── Step 4: Build services ─────────────────────────────────────────────
        DeliveryAssignmentStrategy deliveryStrategy = new FirstAvailableDeliveryAssignment();

        NotificationService notificationService = new NotificationService(userRepository);

        // MenuService receives the live in-memory Menu reference so its
        // getAllCategories() read never touches the database.
        MenuService menuService = new MenuService(menuRepository, this.menu);

        DBDiscountRepository discountRepository = new DBDiscountRepository();

        TieredPercentageDiscount tieredDiscount = discountRepository.load();

        this.discountService = new DiscountService(tieredDiscount);

        this.orderService = new OrderService(orderRepository, discountService, userRepository,
                deliveryStrategy, notificationService);

        this.authService = new AuthService(userRepository, orderService);

        DeliveryPartnerService deliveryService =
                new DeliveryPartnerService(userRepository, orderService);

        // ── Step 5: Build controllers ──────────────────────────────────────────
        this.adminController = new AdminController(menuService, deliveryService, discountService,
                orderService, this.menu, authService, discountRepository, cartRepository);

        this.customerController = new CustomerController(orderService, this.menu, authService,
                cartRepository, discountService);

        this.deliveryPartnerController =
                new DeliveryPartnerController(orderService, deliveryService, authService);

        // ── Step 6: Ensure a default admin exists ──────────────────────────────
        initializeAdminIfNotExists();
    }

    /** Ensures exactly one ADMIN user exists in the database on first boot. */
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