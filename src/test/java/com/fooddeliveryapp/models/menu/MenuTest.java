package com.fooddeliveryapp.models.menu;

import com.fooddeliveryapp.exception.DuplicateEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuTest {

    private Menu menu;

    @BeforeEach
    void setUp() {
        menu = new Menu("My Restaurant");
    }

    @Test
    void testConstruction() {
        assertNotNull(menu.getId());
        assertNotNull(menu.getRootCategory());
        assertEquals("My Restaurant", menu.getRootCategory().getName());
    }

    @Test
    void testRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Menu(""));
    }

    @Test
    void testAddCategory() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        assertEquals(1, menu.getRootCategory().getComponents().size());
    }

    @Test
    void testRejectsDuplicateCategory() {
        menu.addCategory(new MenuCategory("Starters"));
        assertThrows(DuplicateEntityException.class,
                () -> menu.addCategory(new MenuCategory("Starters")));
    }

    @Test
    void testAddItemToCategory() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);
        menu.addItemToCategory(cat, item);

        // Verify item is in the category
        assertEquals(1, cat.getComponents().size());
    }

    @Test
    void testFindItemById() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);
        menu.addItemToCategory(cat, item);

        MenuItem found = menu.findItemById(item.getId());
        assertNotNull(found);
        assertEquals("Soup", found.getName());
    }

    @Test
    void testFindItemByIdReturnsNullForMissing() {
        assertNull(menu.findItemById("nonexistent"));
    }

    @Test
    void testUpdateItem() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);
        menu.addItemToCategory(cat, item);

        menu.updateItem(item.getId(), 150);
        assertEquals(150, menu.findItemById(item.getId()).getPrice(), 0.01);
    }

    @Test
    void testRemoveItem() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        MenuItem item = new MenuItem("Soup", 100);
        menu.addItemToCategory(cat, item);

        menu.removeItem(item.getId());
        assertNull(menu.findItemById(item.getId()));
    }

    @Test
    void testRemoveCategory() {
        menu.addCategory(new MenuCategory("Starters"));
        menu.removeCategory("Starters");
        assertTrue(menu.getRootCategory().getComponents().isEmpty());
    }

    @Test
    void testRemoveCategoryThrowsForMissing() {
        assertThrows(RuntimeException.class, () -> menu.removeCategory("Nonexistent"));
    }

    @Test
    void testDisplayIndexedMenu() {
        MenuCategory cat = new MenuCategory("Starters");
        menu.addCategory(cat);
        menu.addItemToCategory(cat, new MenuItem("Soup", 100));
        menu.addItemToCategory(cat, new MenuItem("Salad", 80));

        List<MenuItem> items = menu.displayIndexedMenu();
        assertEquals(2, items.size());
    }

    @Test
    void testJdbcConstruction() {
        MenuCategory root = new MenuCategory("root-id", "Restaurant");
        Menu loaded = new Menu("menu-id", root);
        assertEquals("menu-id", loaded.getId());
        assertEquals("Restaurant", loaded.getRootCategory().getName());
    }
}
