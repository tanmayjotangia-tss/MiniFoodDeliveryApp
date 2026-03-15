package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.notification.AppNotification;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.repository.CartRepository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.helper.InvoicePrinter;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.services.payment.PaymentFactory;
import com.fooddeliveryapp.services.payment.PaymentStrategy;
import com.fooddeliveryapp.utils.InputUtil;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class CustomerController {

    private final OrderService orderService;
    private final Menu menu;
    private final AuthService authService;
    private final CartRepository cartRepository;
    private final DiscountService discountService;
    private Cart cart;
    private Customer loggedInCustomer;

    public CustomerController(OrderService orderService, Menu menu, AuthService authService, CartRepository cartRepository, DiscountService discountService) {
        this.orderService = orderService;
        this.menu = menu;
        this.authService = authService;
        this.cartRepository = cartRepository;
        this.discountService = discountService;
    }

    public void start() {
        boolean running = true;
        while (running) {

            if (loggedInCustomer == null) {
                running = showPreLoginMenu();
            } else {
                running = showDashboardMenu();
            }
        }
    }

    private boolean showPreLoginMenu() {

        System.out.println("\n====== CUSTOMER PANEL ======");
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

            default -> System.out.println("Invalid option.");
        }

        return true;
    }

    private boolean showDashboardMenu() {

        // Cart is loaded from DB once at login and kept in memory for the
        // entire session — no need to re-query on every menu iteration.

        System.out.println("\n====== CUSTOMER DASHBOARD ======");
        System.out.println("1. Add Item");
        System.out.println("2. Update Item Quantity");
        System.out.println("3. View Cart");
        System.out.println("4. Clear Cart");
        System.out.println("5. Checkout");
        System.out.println("6. View Orders");
        System.out.println("7. View Notifications");
        System.out.println("8. Logout");
        System.out.println("9. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> addItem();

            case 2 -> removeItem();

            case 3 -> viewCart();

            case 4 -> clearCart();

            case 5 -> checkout();

            case 6 -> viewOrderHistory();

            case 7 -> viewNotifications();

            case 8 -> logout();

            case 9 -> {
                return false;
            }

            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    private void viewNotifications() {

        List<AppNotification> notifications = new ArrayList<>(loggedInCustomer.getNotifications());

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
                        loggedInCustomer.removeNotification(id);
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
                    loggedInCustomer.clearNotifications();
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

    private void addItemToCart(Cart cart) {

        List<MenuItem> items = menu.displayIndexedMenu();

        if (items.isEmpty()) {
            System.out.println("No items available.");
            return;
        }

        int selection = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (selection == 0) return;

        if (selection < 1 || selection > items.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        MenuItem selectedItem = items.get(selection - 1);

        int quantity;
        while (true) {
            quantity = InputUtil.readInt("Enter quantity: ");
            if (quantity > 0) break;
            System.out.println("Quantity must be greater than 0.");
        }

        try{
            cart.addItem(selectedItem, quantity);
            System.out.println("Item added to cart.");

        }catch (InvalidOperationException e){
            System.out.println(e.getMessage());
        }
    }

    private void removeItemFromCart(Cart cart) {

        List<CartItem> items = cart.getItems();

        if (items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        System.out.println("\nSelect Item:");

        for (int i = 0; i < items.size(); i++) {
            CartItem ci = items.get(i);
            System.out.println((i + 1) + ". " + ci.getItem().getName() + " x" + ci.getQuantity());
        }

        int selection = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (selection == 0) return;

        if (selection < 1 || selection > items.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        CartItem selected = items.get(selection - 1);

        System.out.println("1. Decrease Quantity");
        System.out.println("2. Remove Completely");

        int option = InputUtil.readInt("Enter choice: ");

        switch (option) {

            case 1 -> {

                while (true) {

                    int qty = InputUtil.readInt("Enter quantity to decrease: ");

                    if (qty <= 0) {
                        System.out.println("Quantity must be greater than 0.");
                        continue;
                    }

                    try {
                        cart.decreaseItemQuantity(selected.getItem().getId(), qty);
                        System.out.println("Quantity updated.");
                        break;
                    } catch (InvalidOperationException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                }
            }

            case 2 -> {
                try{
                    cart.removeItem(selected.getItem().getId());
                    System.out.println("Item removed from cart.");
                }catch (InvalidOperationException e){
                    System.out.println(e.getMessage());
                }
            }

            default -> System.out.println("Invalid option.");
        }
    }


    private void checkout(Cart cart) {

        if (cart.getItems().isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        System.out.println("\n====== SELECT PAYMENT MODE ======");
        System.out.println("1. Cash");
        System.out.println("2. UPI");

        int choice = InputUtil.readInt("Enter choice: ");

        PaymentMode mode;
        PaymentStrategy strategy;

        switch (choice) {

            case 1 -> {
                mode = PaymentMode.CASH;
                strategy = PaymentFactory.getStrategy("CASH");
            }

            case 2 -> {
                mode = PaymentMode.UPI;

                InputUtil.readUPI("Enter UPI ID: ");

                strategy = PaymentFactory.getStrategy("UPI");
            }

            default -> {
                System.out.println("Invalid payment option.");
                return;
            }
        }
        try{

            Order order = orderService.checkoutCart(cart, strategy, mode);

            System.out.println("Payment Successful.");
            new InvoicePrinter().print(order);
        }catch (InvalidOperationException e){
            System.out.println(e.getMessage());
        }catch (Exception e){
            System.out.println("Unexpected payment error.");
        }
    }

    private void handleLogin() {

        String email = InputUtil.readEmail("Enter Email: ");
        String password = InputUtil.readPassword("Enter Password: ");

        try {
            User user = authService.login(email, password);

            if (user instanceof Customer customer) {
                loggedInCustomer = customer;

                cart = cartRepository
                        .findByCustomerId(loggedInCustomer.getId())
                        .orElseGet(() -> {
                            Cart newCart = new Cart(loggedInCustomer);
                            cartRepository.save(newCart);
                            return newCart;
                        });

                System.out.println("Customer logged in.");
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
        String address = InputUtil.readString("Enter Address: ");
        String password = InputUtil.readPassword("Enter Password: ");

        System.out.println("Notify via:");
        System.out.println("0. App Only");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Both");

        int choice = InputUtil.readInt("Enter your choice: ");

        Set<NotificationType> preferences = new HashSet<>();

        switch (choice) {
            case 0 -> {
                // No external notification
            }
            case 1 -> preferences.add(NotificationType.EMAIL);
            case 2 -> preferences.add(NotificationType.PHONE);
            case 3 -> {
                preferences.add(NotificationType.EMAIL);
                preferences.add(NotificationType.PHONE);
            }
            default -> {
                System.out.println("Invalid choice.");
                return;
            }
        }

        boolean success = false;

        try {
            success = authService.registerCustomer(name, email, phone, address, password, preferences);

            if (success) {
                System.out.println("Registration successful!");
            } else {
                System.out.println("Email already exists.");
            }
        } catch (Exception e) {
            System.out.println("Email already in use. " + e.getMessage());
        }
    }

    private void logout() {
        loggedInCustomer = null;
        System.out.println("Customer logged out.");
    }

    private void addItem() {
        try {
            addItemToCart(cart);
            cartRepository.save(cart);
        } catch (Exception e) {
            System.out.println("Failed to add item: " + e.getMessage());
        }
    }

    private void removeItem() {
        try {
            removeItemFromCart(cart);
            cartRepository.save(cart);
        } catch (Exception e) {
            System.out.println("Failed to update cart: " + e.getMessage());
        }
    }

    private void clearCart() {
        try {
            cart.clearCart();
            cartRepository.save(cart);
            System.out.println("Cart cleared.");
        } catch (Exception e) {
            System.out.println("Failed to clear cart: " + e.getMessage());
        }
    }

    private void viewCart() {

        try {
            // Cart is maintained in memory throughout the session.
            // No DB read needed — this.cart is always current.

            if (cart.getItems().isEmpty()) {
                System.out.println("Cart is empty.");
                return;
            }

            double total = cart.calculateTotal();
            double discount = discountService.calculateDiscount(total);

            cart.printCart(discount);
        } catch (Exception e) {
            System.out.println("Failed to display cart: " + e.getMessage());
        }
    }

    private void checkout() {
        try {
            checkout(cart);
            // Only clear and save if checkout succeeded (no exception above).
            cart.clearCart();
            cartRepository.save(cart);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
    }

    private void displayOrderHistorySummary(List<Order> orders) {

        System.out.println("============================================================");
        System.out.printf("%30s%n", "ORDER HISTORY");
        System.out.println("============================================================");

        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            System.out.println("============================================================");
            return;
        }

        System.out.printf("%-4s %-20s %-12s %-18s %s%n", "No", "Order ID", "Date", "Status", "Amount");

        System.out.println("------------------------------------------------------------");

        int index = 1;

        for (Order order : orders) {

            String shortId = order.getId().substring(0, 8);
            String date = order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yy"));

            System.out.printf("%-4d %-20s %-12s %-18s %.2f%n", index++, shortId, date, order.getStatus(), order.getFinalAmount());
        }

        System.out.println("============================================================");
    }

    private void viewOrderHistory() {

        List<Order> orders = orderService.getOrdersByCustomer(loggedInCustomer.getId());

        displayOrderHistorySummary(orders);

        if (orders.isEmpty()) return;

        int choice = InputUtil.readInt("Select order (0 to back): ");

        if (choice <= 0 || choice > orders.size()) return;

        displayOrderDetails(orders.get(choice - 1));
    }

    private void displayOrderDetails(Order order) {

        System.out.println("============================================================");
        System.out.printf("%30s%n", "ORDER DETAILS");
        System.out.println("============================================================");

        System.out.println("Order ID   : " + order.getId());
        System.out.println("Date       : " + order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        System.out.println("Status     : " + order.getStatus());

        System.out.println("------------------------------------------------------------");

        System.out.printf("%-28s %-7s %-9s %s%n", "Item Name", "Qty", "Price", "Total");

        System.out.println("------------------------------------------------------------");

        double total = 0;

        for (OrderItem item : order.getItems()) {

            double subtotal = item.subtotal();
            total += subtotal;

            System.out.printf("%-28s %-7d %-9.2f %.2f%n", item.item().getName(), item.quantity(), item.item().getPrice(), subtotal);
        }

        System.out.println("------------------------------------------------------------");

        double discount = total - order.getFinalAmount();

        System.out.printf("%-45s ₹%10.2f%n", "Total Amount:", total);

        if (discount > 0) {
            System.out.printf("%-45s ₹%10.2f%n", "Discount:", discount);
        }

        System.out.printf("%-45s ₹%10.2f%n", "Final Amount:", order.getFinalAmount());

        System.out.println("============================================================");
    }
}