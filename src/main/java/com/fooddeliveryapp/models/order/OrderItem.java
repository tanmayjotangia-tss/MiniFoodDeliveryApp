package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.models.menu.MenuItem;

import java.io.Serializable;

public record OrderItem(MenuItem item, int quantity) implements Serializable {

    public OrderItem {

        if (item == null) throw new IllegalArgumentException("Item required");

        if (quantity <= 0) throw new IllegalArgumentException("Invalid quantity");

    }

    public double subtotal() {
        return item.getPrice() * quantity;
    }

}
