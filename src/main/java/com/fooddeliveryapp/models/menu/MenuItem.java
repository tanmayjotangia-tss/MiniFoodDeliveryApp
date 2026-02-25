package com.fooddeliveryapp.models.menu;

import java.util.UUID;

public class MenuItem extends MenuComponent {

    private final String id;
    private double price;

    public MenuItem(String name, double price) {
        super(name);

        if (price < 0)
            throw new IllegalArgumentException("Invalid price");

        this.id = UUID.randomUUID().toString();
        this.price = price;
    }

    public String getId() {
        return id;
    }

    @Override
    public double getPrice() {
        return price;
    }

    public void updateName(String newName) {

        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Invalid name");

        this.name = newName.trim();
    }

    public void updatePrice(double newPrice) {

        if (newPrice < 0)
            throw new IllegalArgumentException("Invalid price");

        this.price = newPrice;
    }
}