package com.fooddeliveryapp.models.menu;

import com.fooddeliveryapp.exception.DuplicateEntityException;
import com.fooddeliveryapp.exception.EntityNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Menu implements Serializable {

    private final String id;
    private final MenuCategory rootCategory;

    public Menu(String restaurantName) {

        if (restaurantName == null || restaurantName.isBlank())
            throw new IllegalArgumentException("Restaurant name required");

        this.id = UUID.randomUUID().toString();
        this.rootCategory = new MenuCategory(restaurantName);
    }

    public String getId() {
        return id;
    }

    public MenuCategory getRootCategory() {
        return rootCategory;
    }

    public void addCategory(MenuCategory category) {

        boolean exists = rootCategory.getComponents()
                .stream()
                .filter(c -> c instanceof MenuCategory)
                .map(c -> (MenuCategory) c)
                .anyMatch(c -> c.getName()
                        .equalsIgnoreCase(category.getName()));

        if (exists) throw new DuplicateEntityException("Category already exists: " + category.getName());

        rootCategory.add(category);
    }

    public void addItemToCategory(MenuCategory category, MenuItem item) {
        category.add(item);
    }

    public MenuItem findItemById(String itemId) {
        return findItemRecursive(rootCategory, itemId);
    }

    private MenuItem findItemRecursive(MenuCategory category, String itemId) {

        for (MenuComponent component : category.getComponents()) {

            if (component instanceof MenuItem item) {
                if (item.getId().equals(itemId)) return item;
            }

            if (component instanceof MenuCategory subCategory) {
                MenuItem result = findItemRecursive(subCategory, itemId);
                if (result != null) return result;
            }
        }

        throw new EntityNotFoundException("Menu item not found");
    }

    public void updateItem(String itemId, String newName, double newPrice) {

        MenuItem item = findItemById(itemId);

        MenuCategory parentCategory = findParentCategory(rootCategory, itemId);

        if (parentCategory == null) throw new RuntimeException("Parent category not found");

        boolean duplicateName = parentCategory.getComponents().stream().filter(c -> c instanceof MenuItem).map(c -> (MenuItem) c).anyMatch(i -> !i.getId().equals(itemId) && i.getName().equalsIgnoreCase(newName.trim()));

        if (duplicateName) throw new RuntimeException("Item with same name already exists in this category");

        item.updateName(newName);
        item.updatePrice(newPrice);
    }

    public void removeItem(String itemId) {

        MenuCategory parent = findParentCategory(rootCategory, itemId);

        if (parent == null) throw new RuntimeException("Item not found");

        parent.getComponents().stream().filter(c -> c instanceof MenuItem).map(c -> (MenuItem) c).filter(i -> i.getId().equals(itemId)).findFirst().ifPresentOrElse(parent::remove, () -> {
            throw new RuntimeException("Item not found");
        });
    }

    public void removeCategory(String categoryName) {

        MenuComponent category = rootCategory.getComponents()
                .stream()
                .filter(c -> c instanceof MenuCategory)
                .filter(c -> c.getName().equalsIgnoreCase(categoryName.trim()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Category not found"));

        rootCategory.remove(category);
    }

    private MenuCategory findParentCategory(MenuCategory category, String itemId) {

        for (MenuComponent component : category.getComponents()) {

            if (component instanceof MenuItem item) {

                if (item.getId().equals(itemId)) return category;
            }

            if (component instanceof MenuCategory subCategory) {

                MenuCategory result = findParentCategory(subCategory, itemId);

                if (result != null) return result;
            }
        }
        return null;
    }

    public List<MenuItem> getAllItems() {

        List<MenuItem> items = new ArrayList<>();
        collectItems(rootCategory, items);
        return items;
    }

    private void collectItems(MenuComponent component, List<MenuItem> items) {

        if (component instanceof MenuItem item) {
            items.add(item);
            return;
        }

        if (component instanceof MenuCategory category) {
            for (MenuComponent child : category.getComponents()) {
                collectItems(child, items);
            }
        }
    }

    public void displayMenu() {
        System.out.println("\n========== MENU ==========");
        rootCategory.display("");
        System.out.println("==========================\n");
    }
}