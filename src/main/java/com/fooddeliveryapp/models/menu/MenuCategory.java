package com.fooddeliveryapp.models.menu;

import com.fooddeliveryapp.exception.DuplicateEntityException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MenuCategory extends MenuComponent {

    /** Stable identifier – required for PostgreSQL persistence. */
    private final String id;

    private final List<MenuComponent> components = new ArrayList<>();

    /** Standard constructor – generates a fresh UUID. */
    public MenuCategory(String name) {
        super(name);
        this.id = UUID.randomUUID().toString();
    }

    /**
     * JDBC reconstruction constructor.
     * Restores a category that was previously persisted with the given {@code id}.
     */
    public MenuCategory(String id, String name) {
        super(name);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public void add(MenuComponent component) {

        if (component instanceof MenuCategory category) {
            if (nameExists(category.getName(), MenuCategory.class)) {
                throw new DuplicateEntityException("Category already exists: " + category.getName());
            }
        }

        if (component instanceof MenuItem item) {
            if (nameExists(item.getName(), MenuItem.class)) {
                throw new DuplicateEntityException("Item already exists: " + item.getName());
            }
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

    private String normalize(String name) {
        return name.replaceAll("\\s+", "")
                .toLowerCase().trim();
    }

    private boolean nameExists(String newName, Class<?> type) {

        String normalized = normalize(newName);

        return components.stream()
                .filter(type::isInstance)
                .map(c -> type.cast(c))
                .anyMatch(c -> normalize(((MenuComponent) c)
                        .getName()).equals(normalized));
    }
}