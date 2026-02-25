package com.fooddeliveryapp.models.cart;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.Customer;
import com.fooddeliveryapp.models.menu.MenuItem;

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

    public void addItem(MenuItem item, int quantity) {

        Optional<CartItem> existing =
                items.stream()
                        .filter(ci -> ci.getItem().getId()
                                .equals(item.getId()))
                        .findFirst();

        if (existing.isPresent()) {
            existing.get().increaseQuantity(quantity);
        } else {
            items.add(new CartItem(item, quantity));
        }
    }

    public void removeItem(String itemId) {

        CartItem cartItem = items.stream()
                .filter(ci -> ci.getItem().getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new InvalidOperationException("Item not in cart"));

        items.remove(cartItem);
    }

    public void clearCart() {
        items.clear();
    }

    public double calculateTotal() {
        return items.stream()
                .mapToDouble(CartItem::subtotal)
                .sum();
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
}
