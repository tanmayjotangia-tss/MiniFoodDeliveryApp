package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuComponent;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.repository.CartRepository;
import com.fooddeliveryapp.models.repository.DBDiscountRepository;
import com.fooddeliveryapp.models.users.Admin;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.delivery.DeliveryPartnerService;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.menu.MenuService;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AdminController {

    private final MenuService menuService;
    private final DeliveryPartnerService deliveryService;
    private final DiscountService discountService;
    private final OrderService orderService;
    private final Menu menu;
    private final AuthService authService;
    private final DBDiscountRepository discountRepository;
    private final CartRepository cartRepository;
    private Admin loggedInAdmin;

    public AdminController(MenuService menuService, DeliveryPartnerService deliveryService,
                           DiscountService discountService, OrderService orderService,
                           Menu menu, AuthService authService,
                           DBDiscountRepository discountRepository,
                           CartRepository cartRepository) {

        this.menuService        = menuService;
        this.deliveryService    = deliveryService;
        this.discountService    = discountService;
        this.orderService       = orderService;
        this.menu               = menu;
        this.authService        = authService;
        this.discountRepository = discountRepository;
        this.cartRepository     = cartRepository;
    }

    public void start() {

        boolean running = true;

        while (running) {

            if (loggedInAdmin == null) {
                running = showPreLoginMenu();
            } else {
                running = showDashboardMenu();
            }
        }
    }

    private boolean showPreLoginMenu() {

        System.out.println("\n====== ADMIN PANEL ======");
        System.out.println("1. Login");
        System.out.println("2. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> handleLogin();

            case 2 -> {
                return false;
            }

            default -> System.out.println("Invalid option.");
        }

        return true;
    }

    private boolean showDashboardMenu() {

        System.out.println("\n====== ADMIN DASHBOARD ======");
        System.out.println("1. Manage Menu");
        System.out.println("2. Manage Delivery Partners");
        System.out.println("3. Manage Discount");
        System.out.println("4. Manage Orders");
        System.out.println("5. Orders History");
        System.out.println("6. Revenue Summary");
        System.out.println("7. Logout");
        System.out.println("8. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> manageMenu();

            case 2 -> manageDeliveryPartners();

            case 3 -> configureDiscountMenu();

            case 4 -> manageOrders();

            case 5 -> viewOrderHistory();

            case 6 -> showRevenueSummary();

            case 7 -> logout();

            case 8 -> {
                return false;
            }

            default -> System.out.println("Invalid option.");
        }

        return true;
    }

    //    Menu Management
    private void manageMenu() {

        while (true) {

            System.out.println("\n--- MENU MANAGEMENT ---");
            System.out.println("1. View Current Menu");
            System.out.println("2. Add Category");
            System.out.println("3. Add Item");
            System.out.println("4. Update Item");
            System.out.println("5. Remove Item");
            System.out.println("6. Remove Category");
            System.out.println("7. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> displayMenu();
                case 2 -> addCategory();
                case 3 -> addItem();
                case 4 -> updateItem();
                case 5 -> removeItem();
                case 6 -> removeCategory();
                case 7 -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void displayMenu() {

        try {
            List<MenuCategory> categories = menuService.getAllCategories();

            if (categories.isEmpty()) {
                System.out.println("\nNo menu items available.\n");
                return;
            }

            System.out.println("\n============================================================");
            System.out.println("                           MENU");
            System.out.println("============================================================");

            for (MenuCategory category : categories) {

                System.out.println("\n=> " + category.getName() + ":-");

                // 🔹 Column Header (ONLY for items)
                System.out.printf("   %-3s %-30s %10s%n", "", "Item Name", "Price");
                System.out.println("   ---------------------------------------------------------");

                List<MenuItem> items = category.getComponents()
                        .stream()
                        .filter(component -> component instanceof MenuItem)
                        .map(component -> (MenuItem) component)
                        .toList();

                if (items.isEmpty()) {
                    System.out.println("   (No items available)");
                    continue;
                }

                for (int i = 0; i < items.size(); i++) {
                    MenuItem item = items.get(i);

                    System.out.printf("   %-3d %-30s ₹ %8.2f%n", i + 1, item.getName(), item.getPrice());
                }

                System.out.println("------------------------------------------------------------");
            }
            System.out.println("============================================================\n");

        } catch (Exception e) {
            System.out.println("\nNo menu items available. " + e.getMessage() + "\n");
        }
    }

    private List<MenuItem> displayMenuItemsWithIndex() {

        List<MenuItem> items = getAllMenuItems();

        if (items.isEmpty()) {
            System.out.println("No menu items available.");
            return List.of();
        }

        System.out.println("\n--- MENU ITEMS ---");

        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            System.out.printf("%d. %-25s ₹%8.2f%n", i + 1, item.getName(), item.getPrice());
        }

        return items;
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

        MenuCategory selectedCategory = selectCategory();

        if (selectedCategory == null) {
            return;
        }

        String itemName = InputUtil.readString("Enter item name: ");

        while (true) {
            double price = InputUtil.readDouble("Please enter the item price: ");

            if (price <= 0) {
                System.out.println("Enter a valid price (must be > 0).");
                continue;
            }

            try {
                MenuItem item = new MenuItem(itemName, price);
                menuService.addItem(menu, selectedCategory, item);
                System.out.println("Item added successfully.");
                break;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void updateItem() {

        List<MenuItem> items = displayMenuItemsWithIndex();
        if (items.isEmpty()) return;

        int choice = InputUtil.readInt("Select item to update (0 to cancel): ");

        if (choice == 0) return;

        if (choice < 1 || choice > items.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        MenuItem selected = items.get(choice - 1);

        double newPrice;

        while (true) {
            newPrice = InputUtil.readDouble("Enter new price: ");

            if (newPrice <= 0) {
                System.out.println("Price must be greater than 0. Please try again.");
                continue;
            }

            break;
        }

        try {
            menuService.updateItem(menu, selected.getId(), newPrice);
            System.out.println("Item updated successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void removeItem() {

        List<MenuItem> items = displayMenuItemsWithIndex();
        if (items.isEmpty()) return;

        int choice = InputUtil.readInt("Select item to remove (0 to cancel): ");

        if (choice == 0) return;

        if (choice < 1 || choice > items.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        MenuItem selected = items.get(choice - 1);

        try {
            menuService.removeItem(menu, selected.getId());
            cartRepository.removeItemFromAllCarts(selected.getId());

            System.out.println("Item removed successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void removeCategory() {

        MenuCategory selectedCategory = selectCategory();
        if (selectedCategory == null) return;

        try {
            List<MenuItem> itemsToRemove = selectedCategory.getComponents()
                    .stream()
                    .filter(c -> c instanceof MenuItem)
                    .map(c -> (MenuItem) c).toList();

            for (MenuItem item : itemsToRemove) {
                cartRepository.removeItemFromAllCarts(item.getId());
            }

            menuService.removeCategory(menu, selectedCategory.getName());
            System.out.println("Category removed successfully.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private MenuCategory selectCategory() {
        try {
            List<MenuCategory> categories = menuService.getAllCategories();

            if (categories.isEmpty()) {
                System.out.println("No categories available.");
                return null;
            }

            System.out.println("--- SELECT CATEGORY ---");

            for (int i = 0; i < categories.size(); i++) {
                System.out.println((i + 1) + ". " + categories.get(i).getName());
            }

            while (true) {
                int choice = InputUtil.readInt("Select category (0 to cancel): ");

                if (choice == 0) {
                    System.out.println("Cancelled.");
                    return null;
                }

                if (choice > 0 && choice <= categories.size()) {
                    return categories.get(choice - 1);
                }

                System.out.println("Invalid selection. Please try again.");
            }

        } catch (Exception e) {
            System.out.println("Failed to retrieve categories: " + e.getMessage());
            return null;
        }
    }



    //    Delivery Management
    private void manageDeliveryPartners() {
        while (true) {
            System.out.println("\n--- DELIVERY MANAGEMENT ---");
            System.out.println("1. Remove Partner");
            System.out.println("2. Update Basic Pay");
            System.out.println("3. Update Incentive Percentage");
            System.out.println("4. View All Partners");
            System.out.println("5. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> removePartner();
                case 2 -> updatePartnerPay();
                case 3 -> updateIncentivePercentage();
                case 4 -> viewPartners();
                case 5 -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void updateIncentivePercentage() {
        List<DeliveryPartner> partners = deliveryService.getAllPartners();

        if (partners.isEmpty()) {
            System.out.println("No delivery partners present.");
            return;
        }

        for (int i = 0; i < partners.size(); i++) {
            DeliveryPartner p = partners.get(i);
            System.out.println((i + 1) + ". " + p.getName() + " | ID: " + p.getId());
        }
        String id = InputUtil.readString("Enter partner ID: ");
        double percentage = InputUtil.readDouble("Enter new incentive percentage: ");
        try {
            deliveryService.updateIncentivePercentage(id, percentage);
            System.out.println("Incentive Percentage updated.");
        } catch (EntityNotFoundException e) {
            System.out.println(e.getMessage());
        }catch (Exception e) {
            System.out.println("Failed to update incentive: " + e.getMessage());
        }
    }

    private void removePartner() {

        List<DeliveryPartner> partners = deliveryService.getAllPartners();

        if (partners.isEmpty()) {
            System.out.println("No delivery partners present.");
            return;
        }

        DeliveryPartner selected = selectFromList(partners, p -> "Name: " + p.getName() + " | Available: " + p.isAvailable() + " | Basic Pay: ₹" + p.getBasicPay());

        if (selected == null) return;

        boolean hasActiveOrders = orderService.getOrdersByPartner(selected.getId())
                .stream()
                .anyMatch(o -> o.getStatus() == OrderStatus.OUT_FOR_DELIVERY || o.getStatus() == OrderStatus.CONFIRMED_BY_ADMIN || o.getStatus() == OrderStatus.PAID);

        if (hasActiveOrders) {
            System.out.println("Cannot remove partner — they have active deliveries in progress.");
            System.out.println("Wait until all their orders are delivered or cancelled first.");
            return;
        }

        String confirm = InputUtil.readString("Are you sure? (yes/no): ");

        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("Operation cancelled.");
            return;
        }

        try {
            deliveryService.removePartner(selected.getId());
            System.out.println("Partner removed successfully.");
        } catch (Exception e) {
            System.out.println("Failed to remove partner: " + e.getMessage());
        }
    }

    private void updatePartnerPay() {
        List<DeliveryPartner> partners = deliveryService.getAllPartners();

        if (partners.isEmpty()) {
            System.out.println("No delivery partners present.");
            return;
        }

        DeliveryPartner selected = selectFromList(partners, p -> "Name: " + p.getName() + " | Basic Pay: ₹" + p.getBasicPay());

        if (selected == null) return;

        double pay;
        while (true) {
            pay = InputUtil.readDouble("Enter new basic pay: ");

            if (pay > 0) {
                break;
            }

            System.out.println("Basic pay must be greater than 0. Please try again.");
        }

        try {
            deliveryService.updateBasicPay(selected.getId(), pay);
            System.out.println("Basic pay updated.");
        }catch(EntityNotFoundException e) {
            System.out.println("Failed to update basic pay: " + e.getMessage());
        }catch (Exception e) {
            System.out.println("Failed to update basic pay: " + e.getMessage());
        }
    }

    private void viewPartners() {
        List<DeliveryPartner> partners = deliveryService.getAllPartners();
        if (partners.isEmpty()) {
            System.out.println("No delivery partners present.");
            return;
        }

        System.out.println("\n--- DELIVERY PARTNERS ---");
        for (DeliveryPartner partner : partners) {
            System.out.println("ID: " + partner.getId() + " | Name: " + partner.getName() + " | Available: " + partner.isAvailable());
        }
    }


    //    Discount Management
    private void configureDiscountMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n=== CONFIGURE DISCOUNT ===");
            System.out.println("1. Add Discount Slab");
            System.out.println("2. Remove Discount Slab");
            System.out.println("3. View Discount Slabs");
            System.out.println("4. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {
                case 1 -> addDiscountSlab();
                case 2 -> removeDiscountSlab();
                case 3 -> viewDiscountSlabs();
                case 4 -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void addDiscountSlab() {

        double threshold = InputUtil.readDouble("Enter minimum cart amount: ");
        double percentage = InputUtil.readDouble("Enter discount percentage: ");

        try {
            discountService.getTieredDiscount().addSlab(threshold, percentage);

            discountRepository.save(discountService.getTieredDiscount());

            System.out.println("Discount slab added successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void removeDiscountSlab() {

        Map<Double, Double> slabs = discountService.getTieredDiscount().getSlabs();

        if (slabs.isEmpty()) {
            System.out.println("No discount slabs configured.");
            return;
        }

        System.out.println("Existing Slabs:");
        slabs.forEach((threshold, percentage) -> System.out.println("Above ₹" + threshold + " → " + percentage + "%"));

        double threshold = InputUtil.readDouble("Enter threshold to remove: ");

        if (!slabs.containsKey(threshold)) {
            System.out.println("No slab found for that threshold.");
            return;
        }

        try {
            discountService.getTieredDiscount().removeSlab(threshold);
            discountRepository.save(discountService.getTieredDiscount());
            System.out.println("Slab removed successfully.");
        } catch (Exception e) {
            System.out.println("Failed to remove slab: " + e.getMessage());
        }
    }

    private void viewDiscountSlabs() {

        Map<Double, Double> slabs = discountService.getTieredDiscount().getSlabs();

        final int WIDTH = 60;

        System.out.println("============================================================");
        centerText("ACTIVE DISCOUNT SLABS", WIDTH);
        System.out.println("============================================================");

        if (slabs.isEmpty()) {
            System.out.println("No discount slabs configured.");
            System.out.println("============================================================");
            return;
        }

        System.out.printf("%-5s %-20s %-20s%n", "No", "Minimum Cart Amount", "Discount %");

        System.out.println("------------------------------------------------------------");

        int index = 1;

        for (Map.Entry<Double, Double> entry : slabs.entrySet()) {

            System.out.printf("%-5d ₹%-19.2f %-20.2f%%%n", index++, entry.getKey(), entry.getValue());
        }

        System.out.println("============================================================");
    }

    private void centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        System.out.printf("%" + (padding + text.length()) + "s%n", text);
    }

    private void viewOrderHistory() {

        try {
            List<Order> orders = orderService.getAllOrders();

            if (orders.isEmpty()) {
                System.out.println("No delivery orders present.");
                return;
            }

            System.out.println("\n--- ORDER HISTORY ---");

            for (Order order : orders) {
                System.out.println("Order ID: " + order.getId() + " | Customer: " + order.getCustomerName() + " | Final Amount: ₹" + order.getFinalAmount() + " | Status: " + order.getStatus());
            }
        } catch (Exception e) {
            System.out.println("Failed to load order history: " + e.getMessage());
        }
    }

    private void showRevenueSummary() {

        try {
            double revenue = orderService.calculateTotalRevenue();
            long totalOrders = orderService.getTotalOrders();

            System.out.println("\n====== REVENUE SUMMARY ======");
            System.out.println("Total Orders : " + totalOrders);
            System.out.println("Total Revenue: ₹" + revenue);
            System.out.println("=============================");
        } catch (Exception e) {
            System.out.println("Failed to load revenue data: " + e.getMessage());
        }
    }

    private void manageOrders() {

        List<Order> orders = orderService.getAllOrders()
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID
                || o.getStatus() == OrderStatus.CONFIRMED_BY_ADMIN)
                .toList();

        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        System.out.println("\n=== ORDER LIST ===");

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println((i + 1) + ". ID: " + o.getId() + " | Customer: " + o.getCustomerName() + " | Status: " + o.getStatus());
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
        System.out.println("\nManaging Order ID: " + order.getId());
        System.out.println("Current Status: " + order.getStatus());

        System.out.println("1. Order ready to delivery");
        System.out.println("2. Cancel Order");
        System.out.println("3. Back");

        int choice = InputUtil.readInt("Enter choice: ");

        try {

            switch (choice) {

                case 1 -> {
                    try{
                        orderService.confirmOrder(order.getId());
                        System.out.println("Order Confirmed.");
                    }catch (InvalidOperationException e) {
                        System.out.println("Invalid Order ID.");
                    } catch (Exception e) {
                        System.out.println("Invalid Order ID.");
                    }
                }

                case 2 -> {
                    orderService.cancelOrderByAdmin(order.getId());
                    System.out.println("Order Cancelled.");
                }

                case 3 -> {
                }

                default -> System.out.println("Invalid choice.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
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

    private void handleLogin() {

        String email = InputUtil.readEmail("Enter Email: ");
        String password = InputUtil.readPassword("Enter Password: ");

        try {
            User user = authService.login(email, password);

            if (user instanceof Admin admin) {
                loggedInAdmin = admin;
                System.out.println("Admin logged in successfully.");
            } else {
                System.out.println("Invalid admin credentials.");
            }

        } catch (Exception e) {
            System.out.println("Invalid admin credentials. " + e.getMessage());
        }
    }

    private void logout() {
        loggedInAdmin = null;
        System.out.println("Admin logged out.");
    }

    private List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();

        for (MenuComponent component : menu.getRootCategory().getComponents()) {
            if (component instanceof MenuCategory category) {
                for (MenuComponent child : category.getComponents()) {
                    if (child instanceof MenuItem item) {
                        items.add(item);
                    }
                }
            }
        }
        return items;
    }
}