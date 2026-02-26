package com.fooddeliveryapp.models.cart;

import com.fooddeliveryapp.models.menu.MenuItem;

import java.io.Serializable;

public class CartItem implements Serializable {

    private final MenuItem item;
    private int quantity;

    public CartItem(MenuItem item, int quantity) {

        if (item == null) throw new IllegalArgumentException("Menu item required");

        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");

        this.item = item;
        this.quantity = quantity;
    }

    public void increaseQuantity(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Invalid quantity");
        this.quantity += qty;
    }

    public void decreaseQuantity(int qty) {
        if (qty <= 0 || qty > quantity) throw new IllegalArgumentException("Invalid quantity");
        this.quantity -= qty;
    }

    public double subtotal() {
        return item.getPrice() * quantity;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }
}
