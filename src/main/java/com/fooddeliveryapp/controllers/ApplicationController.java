package com.fooddeliveryapp.controllers;

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

    public ApplicationController() {

//      Serialization
        Repository<Menu> menuRepository = new FileRepository<>("menu.dat");

        Repository<Order> orderRepository = new FileRepository<>("orders.dat");

        Repository<DeliveryPartner> partnerRepository = new FileRepository<>("partners.dat");

//        Loading menu
        List<Menu> menus = menuRepository.findAll();

        if (menus.isEmpty()) {
            this.menu = new Menu("My Restaurant");
            menuRepository.save(this.menu);
        } else {
            this.menu = menus.get(0);
        }

//      Discount by default
        DiscountService discountService = new DiscountService(new AmountDiscount(500, 10));

        DeliveryAssignmentStrategy deliveryStrategy = new RandomDeliveryAssignment();

//        Services initialisation
        MenuService menuService = new MenuService(menuRepository);

        DeliveryPartnerService deliveryService = new DeliveryPartnerService(partnerRepository);

        OrderService orderService = new OrderService(orderRepository, discountService, partnerRepository, deliveryStrategy);

//        Controller initialisation
        this.adminController = new AdminController(menuService, deliveryService, discountService, orderService, this.menu);

        this.customerController = new CustomerController(orderService, this.menu);

        this.deliveryPartnerController = new DeliveryPartnerController(orderService, deliveryService);
    }

    private boolean isMenuEmpty() {

        return menu.getRootCategory().getComponents().stream().filter(c -> c instanceof MenuCategory).map(c -> (MenuCategory) c).allMatch(category -> category.getComponents().isEmpty());
    }

    public void start() {

        while (true) {

            System.out.println("\n=== RESTAURANT SYSTEM ===");
            System.out.println("1. Admin");
            System.out.println("2. Customer");
            System.out.println("3. Delivery Partner");
            System.out.println("4. Exit");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> adminController.start();

                case 2 -> {
                    if (isMenuEmpty()) {
                        System.out.println("\nMenu is currently empty.");
                        System.out.println("Please contact admin to add items first.");
                    } else {
                        customerController.start();
                    }
                }

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