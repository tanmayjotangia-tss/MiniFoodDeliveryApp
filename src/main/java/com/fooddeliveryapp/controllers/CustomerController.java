package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.repository.CartRepository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.helper.AuthService;
import com.fooddeliveryapp.services.order.OrderService;
import com.fooddeliveryapp.services.payment.PaymentStrategy;
import com.fooddeliveryapp.services.payment.PaymentFactory;
import com.fooddeliveryapp.services.helper.InvoicePrinter;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomerController {

    private final OrderService orderService;
    private final Menu menu;
    private Cart cart;
    private Customer loggedInCustomer;
    private final AuthService authService;
    private final CartRepository cartRepository;

    public CustomerController(OrderService orderService, Menu menu, AuthService authService, CartRepository cartRepository) {
        this.orderService = orderService;
        this.menu = menu;
        this.authService = authService;
        this.cartRepository = cartRepository;
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

            case 1 -> login();

            case 2 -> register();

            case 3 -> {
                return false;
            }

            default -> System.out.println("Invalid option.");
        }

        return true;
    }

    private boolean showDashboardMenu() {

        System.out.println("\n====== CUSTOMER DASHBOARD ======");
        System.out.println("1. Add Item");
        System.out.println("2. Remove Item");
        System.out.println("3. View Cart");
        System.out.println("4. Checkout");
        System.out.println("5. Logout");
        System.out.println("6. Back to Main Menu");

        int choice = InputUtil.readInt("Enter choice: ");

        switch (choice) {

            case 1 -> addItem();

            case 2 -> removeItem();

            case 3 -> viewCart();

            case 4 -> checkout();

            case 5 -> {
                logout();
            }

            case 6 -> {
                return false;
            }

            default -> System.out.println("Invalid option.");
        }
        return true;
    }

    private void addItemToCart(Cart cart) {

        menu.displayMenu();

        List<MenuItem> items = menu.getAllItems();

        if (items.isEmpty()) {
            System.out.println("No items available.");
            return;
        }

        System.out.println("\nSelect Item:");

        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            System.out.println((i + 1) + ". " + item.getName() + " | ₹" + item.getPrice());
        }

        int selection = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (selection == 0) return;

        if (selection < 1 || selection > items.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        MenuItem selectedItem = items.get(selection - 1);

        int quantity = InputUtil.readInt("Enter quantity: ");

        cart.addItem(selectedItem, quantity);

        System.out.println("Item added to cart.");
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
                int qty = InputUtil.readInt("Enter quantity to decrease: ");
                cart.decreaseItemQuantity(selected.getItem().getId(), qty);
                System.out.println("Quantity updated.");
            }

            case 2 -> {
                cart.removeItem(selected.getItem().getId());
                System.out.println("Item removed.");
            }

            default -> System.out.println("Invalid option.");
        }
    }


    private void checkout(Cart cart) {

        if (cart.getItems().isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        String modeInput = InputUtil.readString("Payment mode (cash/upi): ");

        PaymentMode mode = PaymentMode.valueOf(modeInput.toUpperCase());

        PaymentStrategy strategy = PaymentFactory.getStrategy(String.valueOf(mode));

        Order order = orderService.checkoutCart(cart, strategy, mode);

        System.out.println("Payment Successful.");

        new InvoicePrinter().print(order);
    }

    private void login() {

        String email = InputUtil.readEmail("Enter Email: ");
        String password = InputUtil.readPassword("Enter Password: ");

        User user = authService.login(email, password);

        if (user instanceof Customer customer) {
            loggedInCustomer = customer;
            cart = cartRepository.findByCustomerId(loggedInCustomer.getId()).orElseGet(() -> {
                Cart newCart = new Cart(loggedInCustomer);
                cartRepository.save(newCart);
                return newCart;
            });
            System.out.println("Customer logged in.");
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    private void register() {

        String name = InputUtil.readValidName("Enter Name: ");
        String email = InputUtil.readEmail("Enter Email: ");
        String phone = InputUtil.readPhoneNumber("Enter Phone: ");
        String password = InputUtil.readPassword("Enter Password: ");

        System.out.println("Notify via:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Both");

        int choice = InputUtil.readInt("Enter your choice: ");

        Set<NotificationType> preferences = new HashSet<>();

        switch (choice) {
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

        boolean success = authService.registerCustomer(name, email, phone, password, preferences);

        if (success) {
            System.out.println("Registration successful!");
        } else {
            System.out.println("Email already exists.");
        }
    }

    private void logout() {
        loggedInCustomer = null;
        System.out.println("Customer logged out.");
    }

    private void addItem() {
        addItemToCart(cart);
        cartRepository.save(cart);
    }

    private void removeItem() {
        removeItemFromCart(cart);
        cartRepository.save(cart);
    }

    private void viewCart() {

        if (cart.getItems().isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        cart.printCart();
    }

    private void checkout() {
        checkout(cart);
        cart.clearCart();
        cartRepository.save(cart);

    }
}