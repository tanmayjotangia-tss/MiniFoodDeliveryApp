package com.fooddeliveryapp.models.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuItemTest {

    @Test
    void testConstruction() {
        MenuItem item = new MenuItem("Burger", 150);
        assertNotNull(item.getId());
        assertEquals("Burger", item.getName());
        assertEquals(150, item.getPrice(), 0.01);
    }

    @Test
    void testRejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("Burger", -10));
    }

    @Test
    void testAllowsZeroPrice() {
        MenuItem item = new MenuItem("Free Sample", 0);
        assertEquals(0, item.getPrice(), 0.01);
    }

    @Test
    void testUpdatePrice() {
        MenuItem item = new MenuItem("Burger", 150);
        item.updatePrice(200);
        assertEquals(200, item.getPrice(), 0.01);
    }

    @Test
    void testUpdatePriceRejectsNegative() {
        MenuItem item = new MenuItem("Burger", 150);
        assertThrows(IllegalArgumentException.class, () -> item.updatePrice(-5));
    }

    @Test
    void testJdbcConstruction() {
        MenuItem item = new MenuItem("fixed-id", "Burger", 150);
        assertEquals("fixed-id", item.getId());
        assertEquals("Burger", item.getName());
        assertEquals(150, item.getPrice(), 0.01);
    }

    @Test
    void testRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("", 100));
    }

    @Test
    void testRejectsNullName() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(null, 100));
    }

    @Test
    void testDisplay() {
        MenuItem item = new MenuItem("Burger", 150);
        assertDoesNotThrow(() -> item.display("  "));
    }
}
