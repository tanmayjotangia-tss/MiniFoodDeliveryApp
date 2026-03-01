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

        return null;
    }

    public void updateItem(String itemId, double newPrice) {

        MenuItem item = findItemById(itemId);

        MenuCategory parentCategory = findParentCategory(rootCategory, itemId);

        if (parentCategory == null) throw new RuntimeException("Parent category not found");

        item.updatePrice(newPrice);
    }

    public void removeItem(String itemId) {

        MenuCategory parent = findParentCategory(rootCategory, itemId);

        if (parent == null) throw new RuntimeException("Item not found");

        parent.getComponents()
                .stream()
                .filter(c -> c instanceof MenuItem)
                .map(c -> (MenuItem) c)
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .ifPresentOrElse(parent::remove, () -> {
            throw new RuntimeException("Item not found");
        });
    }

    public void removeCategory(String categoryName) {

        MenuComponent category = rootCategory.getComponents()
                .stream()
                .filter(c -> c instanceof MenuCategory)
                .filter(c -> c.getName()
                        .equalsIgnoreCase(categoryName.trim()))
                .findFirst().orElseThrow(() -> new RuntimeException("Category not found"));

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

    public List<MenuItem> displayIndexedMenu() {

        List<MenuItem> indexedItems = new ArrayList<>();

        printHeader("MY RESTAURANT");

        displayCategoryFormatted(rootCategory, indexedItems);

        System.out.println("============================================================");

        return indexedItems;
    }

    private void displayCategoryFormatted(MenuComponent component,
                                          List<MenuItem> indexedItems) {

        if (component instanceof MenuCategory category) {

            if (!category.getName().equalsIgnoreCase("My Restaurant")) {

                System.out.println();
                printCenteredCategory(category.getName());
                printColumnHeader();
            }

            for (MenuComponent child : category.getComponents()) {
                displayCategoryFormatted(child, indexedItems);
            }

        } else if (component instanceof MenuItem item) {

            indexedItems.add(item);

            int index = indexedItems.size();

            System.out.printf(
                    "%-4d %-28s ₹%8.2f%n",
                    index,
                    item.getName(),
                    item.getPrice()
            );
        }
    }

    private void printHeader(String title) {

        System.out.println("============================================================");
        System.out.printf("%30s%n", title);
        System.out.println("============================================================\n");
    }

    private void printCenteredCategory(String name) {

        String line = "--------------------------- " +
                name.toUpperCase() +
                " --------------------------";

        System.out.println(line);
    }

    private void printColumnHeader() {

        System.out.println();
        System.out.printf("%-4s %-28s %s%n",
                "No",
                "Item Name",
                "Price");

        System.out.println("------------------------------------------------------------");
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