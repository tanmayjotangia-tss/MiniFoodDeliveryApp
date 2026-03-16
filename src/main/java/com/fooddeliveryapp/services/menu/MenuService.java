package com.fooddeliveryapp.services.menu;

import com.fooddeliveryapp.exception.DuplicateEntityException;
import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.repository.Repository;

import java.util.List;

public class MenuService {

    private final Repository<Menu> menuRepository;
    private final Menu menu;

    public MenuService(Repository<Menu> menuRepository, Menu menu) {
        this.menuRepository = menuRepository;
        this.menu = menu;
    }

    public void addCategory(Menu menu, MenuCategory category) {
        try {
            menu.addCategory(category);
            menuRepository.save(menu);
        } catch (DuplicateEntityException e) {
            System.out.println(category.getName() + " already exists!");
        }
    }

    public void addItem(Menu menu, MenuCategory category, MenuItem item) {
        menu.addItemToCategory(category, item);
        menuRepository.save(menu);
    }

    public void updateItem(Menu menu, String itemId, double newPrice) {
        menu.updateItem(itemId, newPrice);
        menuRepository.save(menu);
    }

    public void removeItem(Menu menu, String itemId) {
        menu.removeItem(itemId);
        menuRepository.save(menu);
    }

    public void removeCategory(Menu menu, String category) {
        menu.removeCategory(category);
        menuRepository.save(menu);
    }

    public List<MenuCategory> getAllCategories() {
        return menu.getRootCategory()
                .getComponents()
                .stream()
                .filter(component -> component instanceof MenuCategory)
                .map(component -> (MenuCategory) component)
                .toList();
    }
}