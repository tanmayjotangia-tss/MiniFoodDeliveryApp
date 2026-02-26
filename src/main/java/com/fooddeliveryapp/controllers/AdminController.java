package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.DeliveryPartner;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuComponent;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.services.DeliveryPartnerService;
import com.fooddeliveryapp.services.DiscountService;
import com.fooddeliveryapp.services.MenuService;
import com.fooddeliveryapp.services.OrderService;
import com.fooddeliveryapp.services.discount.AmountDiscount;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;
import java.util.function.Function;

public class AdminController {

    private final MenuService menuService;
    private final DeliveryPartnerService deliveryService;
    private final DiscountService discountService;
    private final OrderService orderService;
    private final Menu menu;

    public AdminController(MenuService menuService, DeliveryPartnerService deliveryService, DiscountService discountService, OrderService orderService, Menu menu) {

        this.menuService = menuService;
        this.deliveryService = deliveryService;
        this.discountService = discountService;
        this.orderService = orderService;
        this.menu = menu;
    }

    public void start() {

        while (true) {

            System.out.println("\n====== ADMIN PANEL ======");
            System.out.println("1. Manage Menu");
            System.out.println("2. Manage Delivery Partners");
            System.out.println("3. Manage Discount");
            System.out.println("4. Manage Orders");
            System.out.println("5. Revenue Summary");
            System.out.println("6. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> manageMenu();
                case 2 -> manageDeliveryPartners();
                case 3 -> manageDiscount();
//                case 4 -> viewOrderHistory();
                case 4 -> manageOrders();
                case 5 -> showRevenueSummary();
                case 6 -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }


    // =========================
    // MENU MANAGEMENT
    // =========================

    private void manageMenu() {

        while (true) {

            System.out.println("\n--- MENU MANAGEMENT ---");
            System.out.println("1. Add Category");
            System.out.println("2. Add Item");
            System.out.println("3. Update Item");
            System.out.println("4. Remove Item");
            System.out.println("5. Remove Category");
            System.out.println("6. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> addCategory();
                case 2 -> addItem();
                case 3 -> updateItem();
                case 4 -> removeItem();
                case 5 -> removeCategory();
                case 6 -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void addCategory() {

        String name = InputUtil.readString("Enter category name: ");

        try {
            MenuCategory category = new MenuCategory(name);
            menuService.addCategory(menu, category);
            System.out.println("Category added successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void addItem() {

        displayMenuStructure();

        String categoryName = InputUtil.readString("Enter category name to add item: ");

        MenuCategory category = findCategoryByName(categoryName);

        if (category == null) {
            System.out.println("Category not found.");
            return;
        }

        String itemName = InputUtil.readString("Enter item name: ");

        double price = InputUtil.readDouble("Enter item price: ");

        try {
            MenuItem item = new MenuItem(itemName, price);
            menuService.addItem(menu, category, item);
            System.out.println("Item added successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateItem() {

        displayMenuItems();

        String itemId = InputUtil.readString("Enter Item ID to update: ");

        String newName = InputUtil.readString("Enter new name: ");

        double newPrice = InputUtil.readDouble("Enter new price: ");

        try {
            menuService.updateItem(menu, itemId, newName, newPrice);
            System.out.println("Item updated successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void removeItem() {

        displayMenuItems();

        String itemId = InputUtil.readString("Enter Item ID to remove: ");

        try {
            menuService.removeItem(menu, itemId);
            System.out.println("Item removed successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void removeCategory() {

        displayMenuStructure();

        String categoryName = InputUtil.readString("Enter category name to remove: ");

        try {
            menuService.removeCategory(menu, categoryName);
            System.out.println("Category removed successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // =========================
    // DELIVERY MANAGEMENT
    // =========================

    private void manageDeliveryPartners() {

        while (true) {

            System.out.println("\n--- DELIVERY MANAGEMENT ---");
            System.out.println("1. Add Partner");
            System.out.println("2. Remove Partner");
            System.out.println("3. Update Basic Pay");
            System.out.println("4. View All Partners");
            System.out.println("5. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> addPartner();
                case 2 -> removePartner();
                case 3 -> updatePartnerPay();
                case 4 -> viewPartners();
                case 5 -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void addPartner() {

        String name = InputUtil.readString("Enter partner name: ");

        double pay = InputUtil.readDouble("Enter basic pay: ");

        try {
            DeliveryPartner partner = new DeliveryPartner(name, pay);

            deliveryService.addPartner(partner);

            System.out.println("Partner added successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void removePartner() {
        List<DeliveryPartner> partners = deliveryService.getAllPartners();

        DeliveryPartner selected = selectFromList(partners, p -> "Name: " + p.getName() + " | ID: " + p.getId());

        if (selected == null) return;

        deliveryService.removePartner(selected.getId());
    }

    private void updatePartnerPay() {

        viewPartners();

        String id = InputUtil.readString("Enter partner ID: ");

        double pay = InputUtil.readDouble("Enter new basic pay: ");

        try {
            deliveryService.updateBasicPay(id, pay);
            System.out.println("Basic pay updated.");
        } catch (EntityNotFoundException e) {
            System.out.println("Partner not found.");
        }
    }

    private void viewPartners() {

        List<DeliveryPartner> partners = deliveryService.getAllPartners();

        System.out.println("\n--- DELIVERY PARTNERS ---");

        for (DeliveryPartner partner : partners) {
            System.out.println("ID: " + partner.getId() + " | Name: " + partner.getName() + " | Available: " + partner.isAvailable());
        }
    }

    // =========================
    // DISCOUNT MANAGEMENT
    // =========================

    private void manageDiscount() {

        double threshold = InputUtil.readDouble("Enter threshold amount: ");

        double percentage = InputUtil.readDouble("Enter discount percentage: ");

        discountService.updateStrategy(new AmountDiscount(threshold, percentage));

        System.out.println("Discount policy updated.");
    }

    // =========================
    // ORDER HISTORY
    // =========================

    private void viewOrderHistory() {

        List<Order> orders = orderService.getAllOrders();

        System.out.println("\n--- ORDER HISTORY ---");

        for (Order order : orders) {
            System.out.println("Order ID: " + order.getId() + " | Customer: " + order.getCustomer().getName() + " | Final Amount: ₹" + order.getFinalAmount() + " | Status: " + order.getStatus());
        }
    }

    private void showRevenueSummary() {

        double revenue = orderService.calculateTotalRevenue();

        long totalOrders = orderService.getTotalOrders();

        System.out.println("\n====== REVENUE SUMMARY ======");
        System.out.println("Total Orders : " + totalOrders);
        System.out.println("Total Revenue: ₹" + revenue);
        System.out.println("=============================");
    }

    // =========================
    // HELPER METHODS
    // =========================

    private void displayMenuStructure() {

        System.out.println("\n--- CURRENT MENU ---");

        for (MenuComponent component : menu.getRootCategory().getComponents()) {

            if (component instanceof MenuCategory category) {
                System.out.println("Category: " + category.getName());
            }
        }
    }

    private void displayMenuItems() {

        System.out.println("\n--- MENU ITEMS ---");

        for (MenuComponent component : menu.getRootCategory().getComponents()) {

            if (component instanceof MenuCategory category) {

                for (MenuComponent item : category.getComponents()) {

                    if (item instanceof MenuItem menuItem) {

                        System.out.println("ID: " + menuItem.getId() + " | Name: " + menuItem.getName() + " | Price: ₹" + menuItem.getPrice());
                    }
                }
            }
        }
    }

    private MenuCategory findCategoryByName(String name) {

        for (MenuComponent component : menu.getRootCategory().getComponents()) {

            if (component instanceof MenuCategory category) {

                if (category.getName().equalsIgnoreCase(name.trim())) return category;
            }
        }
        return null;
    }

    private void manageOrders() {

        List<Order> orders = orderService.getAllOrders();

        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        System.out.println("\n=== ORDER LIST ===");

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println((i + 1) + ". ID: " + o.getId() + " | Customer: " + o.getCustomer().getName() + " | Status: " + o.getStatus());
        }

        int choice = InputUtil.readInt("Select order (0 to back): ");

        if (choice == 0) return;

        if (choice < 1 || choice > orders.size()) {
            System.out.println("Invalid choice.");
            return;
        }

        Order selected = orders.get(choice - 1);

        manageSingleOrder(selected);
    }

    private void manageSingleOrder(Order order) {

        while (true) {

            System.out.println("\nManaging Order ID: " + order.getId());
            System.out.println("Current Status: " + order.getStatus());

            System.out.println("1. Confirm Order");
            System.out.println("2. Assign Delivery Partner");
            System.out.println("3. Cancel Order");
            System.out.println("4. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            try {

                switch (choice) {

                    case 1 -> {
                        orderService.confirmOrder(order.getId());
                        System.out.println("Order Confirmed.");
                    }

                    case 2 -> {
                        List<DeliveryPartner> partners = deliveryService.getAllPartners();

                        DeliveryPartner selectedPartner = selectFromList(partners, p -> "Name: " + p.getName() + " | Available: " + p.isAvailable());

                        if (selectedPartner == null) return;

                        orderService.assignPartner(order.getId(), selectedPartner.getId());

                        System.out.println("Partner assigned successfully.");

                        System.out.println("Partner Assigned.");
                    }

                    case 3 -> {
                        orderService.cancelOrderByAdmin(order.getId());
                        System.out.println("Order Cancelled.");
                    }

                    case 4 -> {
                        return;
                    }

                    default -> System.out.println("Invalid choice.");
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }


    private <T> T selectFromList(List<T> list, Function<T, String> displayMapper) {

        if (list.isEmpty()) {
            System.out.println("No items available.");
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            System.out.println((i + 1) + ". " + displayMapper.apply(list.get(i)));
        }

        int choice = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (choice == 0) return null;

        if (choice < 1 || choice > list.size()) {
            System.out.println("Invalid selection.");
            return null;
        }

        return list.get(choice - 1);
    }
}