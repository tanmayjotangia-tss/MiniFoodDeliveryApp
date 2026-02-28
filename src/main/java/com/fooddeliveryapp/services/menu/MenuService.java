package com.fooddeliveryapp.services.menu;

import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuComponent;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.repository.Repository;

import java.util.List;

public class MenuService {

    private final Repository<Menu> menuRepository;

    public MenuService(Repository<Menu> menuRepository) {
        this.menuRepository = menuRepository;
    }

    public void addCategory(Menu menu, MenuCategory category) {
        menu.addCategory(category);
        menuRepository.save(menu);
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


    public List<MenuComponent> getFullMenu(Menu menu) {
        return menu.getRootCategory().getComponents();
    }

    public List<MenuCategory> getAllCategories() {

        Menu menu = menuRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Menu not found"));

        return menu.getRootCategory()
                .getComponents()
                .stream()
                .filter(component -> component instanceof MenuCategory)
                .map(component -> (MenuCategory) component)
                .toList();
    }
}
