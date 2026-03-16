package com.fooddeliveryapp.services.menu;

import com.fooddeliveryapp.models.menu.Menu;
import com.fooddeliveryapp.models.menu.MenuCategory;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock private Repository<Menu> menuRepository;

    private MenuService menuService;
    private Menu menu;

    @BeforeEach
    void setUp() {
        menu = new Menu("My Restaurant");
        menuService = new MenuService(menuRepository, menu);
    }

    @Test
    void testAddCategory() {
        MenuCategory cat = new MenuCategory("Starters");
        menuService.addCategory(menu, cat);

        assertEquals(1, menu.getRootCategory().getComponents().size());
        verify(menuRepository).save(menu);
    }

    @Test
    void testAddDuplicateCategory() {
        MenuCategory cat1 = new MenuCategory("Starters");
        menuService.addCategory(menu, cat1);

        MenuCategory cat2 = new MenuCategory("Starters");
        menuService.addCategory(menu, cat2);

        // Should only have 1 category (duplicate ignored)
        assertEquals(1, menu.getRootCategory().getComponents().size());
        // save called once for the successful add
        verify(menuRepository, times(1)).save(menu);
    }

    @Test
    void testAddItem() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);

        menuService.addItem(menu, cat, item);

        assertEquals(1, cat.getComponents().size());
        verify(menuRepository).save(menu);
    }

    @Test
    void testUpdateItem() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);
        menu.addItemToCategory(cat, item);

        menuService.updateItem(menu, item.getId(), 150);

        assertEquals(150, menu.findItemById(item.getId()).getPrice(), 0.01);
        verify(menuRepository).save(menu);
    }

    @Test
    void testRemoveItem() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);
        menu.addItemToCategory(cat, item);

        menuService.removeItem(menu, item.getId());

        assertNull(menu.findItemById(item.getId()));
        verify(menuRepository).save(menu);
    }

    @Test
    void testRemoveCategory() {
        menu.addCategory(new MenuCategory("Starters"));

        menuService.removeCategory(menu, "Starters");

        assertTrue(menu.getRootCategory().getComponents().isEmpty());
        verify(menuRepository).save(menu);
    }

    @Test
    void testGetAllCategories() {
        menu.addCategory(new MenuCategory("Starters"));
        menu.addCategory(new MenuCategory("Main Course"));

        List<MenuCategory> categories = menuService.getAllCategories();

        assertEquals(2, categories.size());
    }

    @Test
    void testGetAllCategoriesEmpty() {
        List<MenuCategory> categories = menuService.getAllCategories();
        assertTrue(categories.isEmpty());
    }
}
