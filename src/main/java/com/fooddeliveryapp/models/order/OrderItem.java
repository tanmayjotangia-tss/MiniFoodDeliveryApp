package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.models.menu.MenuItem;

import java.io.Serializable;

public class OrderItem implements Serializable {

    private final MenuItem item;
    private final int quantity;

    public OrderItem(MenuItem item, int quantity) {

        if (item == null) throw new IllegalArgumentException("Item required");

        if (quantity <= 0) throw new IllegalArgumentException("Invalid quantity");

        this.item = item;
        this.quantity = quantity;
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
