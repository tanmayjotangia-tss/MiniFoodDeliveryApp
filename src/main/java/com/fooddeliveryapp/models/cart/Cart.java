package com.fooddeliveryapp.models.cart;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.users.Customer;

import java.io.Serializable;
import java.util.*;

public class Cart implements Serializable {

    private final String id;
    private final Customer customer;
    private final List<CartItem> items = new ArrayList<>();

    public Cart(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Customer required");

        this.id = UUID.randomUUID().toString();
        this.customer = customer;
    }

    public void addItem(MenuItem item, int quantity) {

        if (quantity <= 0) throw new InvalidOperationException("Quantity must be positive");

        Optional<CartItem> existing = items.stream().filter(ci -> ci.getItem().getId().equals(item.getId())).findFirst();

        if (existing.isPresent()) {
            existing.get().increaseQuantity(quantity);
        } else {
            items.add(new CartItem(item, quantity));
        }
    }

    public void decreaseItemQuantity(String itemId, int quantity) {

        CartItem cartItem = items.stream().filter(ci -> ci.getItem().getId().equals(itemId)).findFirst().orElseThrow(() -> new InvalidOperationException("Item not in cart"));

        if (quantity <= 0) throw new InvalidOperationException("Invalid quantity");

        if (quantity >= cartItem.getQuantity()) {
            // remove completely
            items.remove(cartItem);
        } else {
            cartItem.decreaseQuantity(quantity);
        }
    }

    public void removeItem(String itemId) {

        boolean removed = items.removeIf(ci -> ci.getItem().getId().equals(itemId));

        if (!removed) {
            throw new InvalidOperationException("Item not in cart");
        }
    }

    public void clearCart() {
        items.clear();
    }

    public double calculateTotal() {
        return items.stream().mapToDouble(CartItem::subtotal).sum();
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getId() {
        return id;
    }

    public void printCart() {
        if (items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        final int WIDTH = 60;

        printLine('=');
        centerText("YOUR CART", WIDTH);
        printLine('=');

        System.out.printf("%-4s %-20s %-6s %-10s %-10s%n",
                "No", "Item", "Qty", "Price", "Subtotal");
        printLine('-');

        int index = 1;
        double total = 0;

        for (CartItem ci : items) {
            String name = ci.getItem().getName();
            int qty = ci.getQuantity();
            double price = ci.getItem().getPrice();
            double subtotal = ci.subtotal();

            total += subtotal;

            System.out.printf("%-4d %-20s %-6d %-10.2f %-10.2f%n",
                    index++,
                    trim(name, 20),
                    qty,
                    price,
                    subtotal);
        }

        printLine('-');

        System.out.printf("%-42s ₹%10.2f%n", "Total Amount:", total);

        printLine('=');
    }

    private void printLine(char ch) {
        for (int i = 0; i < 60; i++) {
            System.out.print(ch);
        }
        System.out.println();
    }

    private void centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        if (padding < 0) padding = 0;
        System.out.printf("%" + (padding + text.length()) + "s%n", text);
    }

    private String trim(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}