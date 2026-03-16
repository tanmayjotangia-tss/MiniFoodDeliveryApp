package com.fooddeliveryapp.models.menu;

import com.fooddeliveryapp.exception.DuplicateEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuCategoryTest {

    private MenuCategory category;

    @BeforeEach
    void setUp() {
        category = new MenuCategory("Starters");
    }

    @Test
    void testConstruction() {
        assertNotNull(category.getId());
        assertEquals("Starters", category.getName());
        assertTrue(category.getComponents().isEmpty());
    }

    @Test
    void testJdbcConstruction() {
        MenuCategory c = new MenuCategory("cat-id", "Starters");
        assertEquals("cat-id", c.getId());
        assertEquals("Starters", c.getName());
    }

    @Test
    void testAddItem() {
        MenuItem item = new MenuItem("Soup", 100);
        category.add(item);
        assertEquals(1, category.getComponents().size());
    }

    @Test
    void testAddSubCategory() {
        MenuCategory sub = new MenuCategory("Hot Starters");
        category.add(sub);
        assertEquals(1, category.getComponents().size());
    }

    @Test
    void testRejectsDuplicateCategory() {
        category.add(new MenuCategory("Soups"));
        assertThrows(DuplicateEntityException.class,
                () -> category.add(new MenuCategory("Soups")));
    }

    @Test
    void testRejectsDuplicateItem() {
        category.add(new MenuItem("Soup", 100));
        assertThrows(DuplicateEntityException.class,
                () -> category.add(new MenuItem("Soup", 120)));
    }

    @Test
    void testRemoveComponent() {
        MenuItem item = new MenuItem("Soup", 100);
        category.add(item);
        category.remove(item);
        assertTrue(category.getComponents().isEmpty());
    }

    @Test
    void testDisplay() {
        category.add(new MenuItem("Soup", 100));
        assertDoesNotThrow(() -> category.display(""));
    }

    @Test
    void testRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new MenuCategory(""));
    }

    @Test
    void testGetComponentsReturnsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> category.getComponents().add(new MenuItem("Hack", 10)));
    }
}
