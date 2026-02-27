package com.fooddeliveryapp.models.menu;

import com.fooddeliveryapp.exception.DuplicateEntityException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuCategory extends MenuComponent {

    private final List<MenuComponent> components = new ArrayList<>();

    public MenuCategory(String name) {
        super(name);
    }

    @Override
    public void add(MenuComponent component) {

        if (component instanceof MenuCategory category) {

            boolean exists = components.stream()
                    .filter(c -> c instanceof MenuCategory)
                    .map(c -> (MenuCategory) c)
                    .anyMatch(c -> c.getName()
                            .equalsIgnoreCase(category.getName()));

            if (exists) throw new DuplicateEntityException("Category already exists: " + category.getName());
        }

        if (component instanceof MenuItem item) {

            boolean exists = components.stream()
                    .filter(c -> c instanceof MenuItem)
                    .map(c -> (MenuItem) c)
                    .anyMatch(i -> i.getName().equalsIgnoreCase(item.getName()));

            if (exists) throw new DuplicateEntityException("Item already exists: " + item.getName());
        }

        components.add(component);
    }

    @Override
    public void remove(MenuComponent component) {
        components.remove(component);
    }

    @Override
    public void display(String indent) {

        System.out.println(indent + "Category: " + name);

        for (MenuComponent component : components) {
            component.display(indent + "   ");
        }
    }

    public List<MenuComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }
}