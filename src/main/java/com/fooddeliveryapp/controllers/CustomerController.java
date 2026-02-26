package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.services.OrderService;
import com.fooddeliveryapp.services.payment.PaymentStrategy;
import com.fooddeliveryapp.services.payment.PaymentFactory;
import com.fooddeliveryapp.services.InvoicePrinter;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;

public class CustomerController {

    private final OrderService orderService;
    private final Menu menu;

    public CustomerController(OrderService orderService, Menu menu) {
        this.orderService = orderService;
        this.menu = menu;
    }

    public void start() {

        String name = InputUtil.readString("Enter your name: ");

        String email = InputUtil.readString("Enter your email: ");

        Customer customer = new Customer(name, email);

        Cart cart = new Cart(customer);

        while (true) {

            menu.getAllItems();

            System.out.println("1. Add Item");
            System.out.println("2. Remove Item");
            System.out.println("3. View Cart");
            System.out.println("4. Checkout");
            System.out.println("5. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> addItemToCart(cart);

                case 2 -> removeItemFromCart(cart);

                case 3 -> cart.printCart();

                case 4 -> checkout(cart);

                case 5 -> {
                    return;
                }

                default -> System.out.println("Invalid choice.");
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

        System.out.println("\nSelect Item to Remove:");

        for (int i = 0; i < items.size(); i++) {
            CartItem ci = items.get(i);
            System.out.println((i + 1) + ". " + ci.getItem() + " x" + ci.getQuantity());
        }

        int selection = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (selection == 0) return;

        if (selection < 1 || selection > items.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        CartItem selected = items.get(selection - 1);

        cart.removeItem(String.valueOf(selected.getItem()));

        System.out.println("Item removed from cart.");
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
}