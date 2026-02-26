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
        if (customer == null)
            throw new IllegalArgumentException("Customer required");

        this.id = UUID.randomUUID().toString();
        this.customer = customer;
    }

    // --------------------------------------------------
    // ADD ITEM (Increase quantity if already exists)
    // --------------------------------------------------
    public void addItem(MenuItem item, int quantity) {

        if (quantity <= 0)
            throw new InvalidOperationException("Quantity must be positive");

        Optional<CartItem> existing = items.stream()
                .filter(ci -> ci.getItem().getId().equals(item.getId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().increaseQuantity(quantity);
        } else {
            items.add(new CartItem(item, quantity));
        }
    }

    // --------------------------------------------------
    // DECREASE QUANTITY
    // --------------------------------------------------
    public void decreaseItemQuantity(String itemId, int quantity) {

        CartItem cartItem = items.stream()
                .filter(ci -> ci.getItem().getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new InvalidOperationException("Item not in cart"));

        if (quantity <= 0)
            throw new InvalidOperationException("Invalid quantity");

        if (quantity >= cartItem.getQuantity()) {
            // remove completely
            items.remove(cartItem);
        } else {
            cartItem.decreaseQuantity(quantity);
        }
    }

    // --------------------------------------------------
    // REMOVE ITEM COMPLETELY
    // --------------------------------------------------
    public void removeItem(String itemId) {

        boolean removed = items.removeIf(
                ci -> ci.getItem().getId().equals(itemId)
        );

        if (!removed) {
            throw new InvalidOperationException("Item not in cart");
        }
    }

    // --------------------------------------------------
    // CLEAR CART
    // --------------------------------------------------
    public void clearCart() {
        items.clear();
    }

    // --------------------------------------------------
    // TOTAL
    // --------------------------------------------------
    public double calculateTotal() {
        return items.stream()
                .mapToDouble(CartItem::subtotal)
                .sum();
    }

    // --------------------------------------------------
    // GETTERS
    // --------------------------------------------------
    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getId() {
        return id;
    }

    // --------------------------------------------------
    // PRINT
    // --------------------------------------------------
    public void printCart() {

        if (items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        System.out.println("\n--- YOUR CART ---");

        for (int i = 0; i < items.size(); i++) {
            CartItem ci = items.get(i);
            System.out.println((i + 1) + ". "
                    + ci.getItem().getName()
                    + " x" + ci.getQuantity()
                    + " = ₹" + ci.subtotal());
        }

        System.out.println("Total: ₹" + calculateTotal());
    }
}