package com.fooddeliveryapp.models.cart;

import com.fooddeliveryapp.models.menu.MenuItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    private MenuItem sampleItem() {
        return new MenuItem("Burger", 150);
    }

    @Test
    void testConstruction() {
        CartItem ci = new CartItem(sampleItem(), 2);
        assertEquals("Burger", ci.getItem().getName());
        assertEquals(2, ci.getQuantity());
    }

    @Test
    void testRejectsNullItem() {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(null, 1));
    }

    @Test
    void testRejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(sampleItem(), 0));
    }

    @Test
    void testRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(sampleItem(), -1));
    }

    @Test
    void testIncreaseQuantity() {
        CartItem ci = new CartItem(sampleItem(), 2);
        ci.increaseQuantity(3);
        assertEquals(5, ci.getQuantity());
    }

    @Test
    void testIncreaseRejectsInvalid() {
        CartItem ci = new CartItem(sampleItem(), 2);
        assertThrows(IllegalArgumentException.class, () -> ci.increaseQuantity(0));
    }

    @Test
    void testDecreaseQuantity() {
        CartItem ci = new CartItem(sampleItem(), 5);
        ci.decreaseQuantity(2);
        assertEquals(3, ci.getQuantity());
    }

    @Test
    void testDecreaseRejectsExceedingQuantity() {
        CartItem ci = new CartItem(sampleItem(), 2);
        assertThrows(IllegalArgumentException.class, () -> ci.decreaseQuantity(3));
    }

    @Test
    void testSubtotal() {
        CartItem ci = new CartItem(sampleItem(), 3);
        assertEquals(450, ci.subtotal(), 0.01);
    }
}
