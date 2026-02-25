package com.fooddeliveryapp.models.menu;

import java.io.Serializable;

public abstract class MenuComponent implements Serializable {

    protected String name;

    public MenuComponent(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be empty");
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public void add(MenuComponent component) {
        throw new UnsupportedOperationException();
    }

    public void remove(MenuComponent component) {
        throw new UnsupportedOperationException();
    }

    public double getPrice() {
        throw new UnsupportedOperationException();
    }
}
