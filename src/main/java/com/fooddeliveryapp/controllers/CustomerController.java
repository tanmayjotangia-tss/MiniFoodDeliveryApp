package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.Customer;
import com.fooddeliveryapp.models.PaymentMode;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuComponent;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.services.InvoicePrinter;
import com.fooddeliveryapp.services.OrderService;
import com.fooddeliveryapp.services.payment.PaymentFactory;
import com.fooddeliveryapp.services.payment.PaymentStrategy;
import com.fooddeliveryapp.utils.InputUtil;

public class CustomerController {

    private final OrderService orderService;
    private final Menu menu;
    private final InvoicePrinter invoicePrinter = new InvoicePrinter();

    public CustomerController(OrderService orderService, Menu menu) {
        this.orderService = orderService;
        this.menu = menu;
    }

    public void start() {

        String name = InputUtil.readValidName("Enter your name: ");
        String email = InputUtil.readEmail("Enter your email: ");

        Customer customer = new Customer(name, email);

        Cart cart = new Cart(customer);

        while (true) {

            displayMenu();

            System.out.println("\n1. Add Item");
            System.out.println("2. Remove Item");
            System.out.println("3. View Cart");
            System.out.println("4. Checkout");

            int choice = InputUtil.readInt("Enter choice: ");

            switch (choice) {

                case 1 -> {
                    String itemId = InputUtil.readString("Enter Item ID: ");
                    int qty = InputUtil.readInt("Enter quantity: ");

                    MenuItem item = menu.findItemById(itemId);
                    cart.addItem(item, qty);
                }

                case 2 -> {
                    String itemId = InputUtil.readString("Enter Item ID to remove: ");
                    cart.removeItem(itemId);
                }

                case 3 -> printCart(cart);

                case 4 -> {
                    String paymentInput =
                            InputUtil.readString("Payment mode (cash/upi): ");

                    PaymentMode mode =
                            PaymentMode.valueOf(paymentInput.toUpperCase());

                    PaymentStrategy strategy =
                            PaymentFactory.getStrategy(paymentInput);

                    Order order = orderService.checkoutCart(
                            cart, strategy, mode);

                    new InvoicePrinter().print(order);
                    return;
                }

                default -> System.out.println("Invalid option");
            }
        }
    }

    private void displayMenu() {

        System.out.println("\n--- MENU ---");

        for (MenuComponent component :
                menu.getRootCategory().getComponents()) {

            if (component instanceof MenuCategory category) {

                System.out.println("\nCategory: " + category.getName());

                for (MenuComponent item : category.getComponents()) {
                    if (item instanceof MenuItem menuItem) {
                        System.out.println("ID: " + menuItem.getId()
                                + " | " + menuItem.getName()
                                + " | ₹" + menuItem.getPrice());
                    }
                }
            }
        }
    }
    private void printCart(Cart cart) {

        System.out.println("\n--- YOUR CART ---");

        for (CartItem item : cart.getItems()) {

            System.out.println(
                    item.getItem().getName() +
                            " x" + item.getQuantity() +
                            " = ₹" + item.subtotal()
            );
        }

        System.out.println("Total: ₹" + cart.calculateTotal());
    }
}
