package com.fooddeliveryapp.models.menu;

import java.util.UUID;

public class MenuItem extends MenuComponent {

    private final String id;
    private double price;

    public MenuItem(String name, double price) {
        super(name);

        if (price < 0) throw new IllegalArgumentException("Invalid price");

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


    @Override
    public void display(String indent) {
        System.out.printf("%s- %-25s ₹%8.2f%n",
                indent,
                trim(name, 25),
                price);
    }

    private String trim(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public void updatePrice(double newPrice) {

        if (newPrice < 0) throw new IllegalArgumentException("Invalid price");

        this.price = newPrice;
    }
}