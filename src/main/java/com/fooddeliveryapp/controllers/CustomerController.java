package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.repository.CartRepository;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.services.AuthService;
import com.fooddeliveryapp.services.OrderService;
import com.fooddeliveryapp.services.payment.PaymentStrategy;
import com.fooddeliveryapp.services.payment.PaymentFactory;
import com.fooddeliveryapp.services.InvoicePrinter;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;

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

        while (true) {

            System.out.println("\n====== CUSTOMER PANEL ======");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Add Item");
            System.out.println("4. Remove Item");
            System.out.println("5. View Cart");
            System.out.println("6. Checkout");
            System.out.println("7. Logout");
            System.out.println("8. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> login();
                case 2 -> register();
                case 3 -> requireLogin(this::addItem);
                case 4 -> requireLogin(this::removeItem);
                case 5 -> requireLogin(this::viewCart);
                case 6 -> requireLogin(this::checkout);
                case 7 -> logout();
                case 8 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
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
            System.out.println((i + 1) + ". "
                    + ci.getItem().getName()
                    + " x" + ci.getQuantity());
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
                cart.decreaseItemQuantity(
                        selected.getItem().getId(),
                        qty
                );
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

        String email = InputUtil.readString("Enter Email: ");
        String password = InputUtil.readString("Enter Password: ");

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
    }

    private void requireLogin(Runnable action) {

        if (loggedInCustomer == null) {
            System.out.println("Please login first.");
            return;
        }

        action.run();
    }

    private void register() {

        String name = InputUtil.readString("Enter Name: ");
        String email = InputUtil.readString("Enter Email: ");
        String phone = InputUtil.readString("Enter Phone: ");
        String password = InputUtil.readString("Enter Password: ");

        authService.register(name, email, phone, password, "CUSTOMER");
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