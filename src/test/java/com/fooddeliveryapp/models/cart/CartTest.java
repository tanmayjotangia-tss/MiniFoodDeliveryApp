package com.fooddeliveryapp.models.cart;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.users.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    private Cart cart;
    private Customer customer;
    private MenuItem burger;
    private MenuItem pizza;

    @BeforeEach
    void setUp() {
        customer = new Customer("John", "john@test.com", "9876543210",
                "123 Main St", "Pass@123", EnumSet.noneOf(NotificationType.class));
        cart = new Cart(customer);
        burger = new MenuItem("Burger", 150);
        pizza = new MenuItem("Pizza", 300);
    }

    @Test
    void testConstruction() {
        assertNotNull(cart.getId());
        assertEquals(customer, cart.getCustomer());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testRejectsNullCustomer() {
        assertThrows(IllegalArgumentException.class, () -> new Cart(null));
    }

    @Test
    void testAddItem() {
        cart.addItem(burger, 2);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void testAddItemIncreasesExistingQuantity() {
        cart.addItem(burger, 2);
        cart.addItem(burger, 3);
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
    }

    @Test
    void testAddItemRejectsZeroQuantity() {
        assertThrows(InvalidOperationException.class, () -> cart.addItem(burger, 0));
    }

    @Test
    void testDecreaseItemQuantity() {
        cart.addItem(burger, 5);
        cart.decreaseItemQuantity(burger.getId(), 2);
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test
    void testDecreaseRemovesItemWhenExceeds() {
        cart.addItem(burger, 2);
        cart.decreaseItemQuantity(burger.getId(), 5);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testDecreaseThrowsForMissingItem() {
        assertThrows(InvalidOperationException.class,
                () -> cart.decreaseItemQuantity("nonexistent", 1));
    }

    @Test
    void testRemoveItem() {
        cart.addItem(burger, 2);
        cart.removeItem(burger.getId());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testRemoveItemThrowsForMissing() {
        assertThrows(InvalidOperationException.class, () -> cart.removeItem("nonexistent"));
    }

    @Test
    void testRemoveItemIfExistsNoThrow() {
        assertDoesNotThrow(() -> cart.removeItemIfExists("nonexistent"));
    }

    @Test
    void testClearCart() {
        cart.addItem(burger, 2);
        cart.addItem(pizza, 1);
        cart.clearCart();
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testCalculateTotal() {
        cart.addItem(burger, 2); // 300
        cart.addItem(pizza, 1);  // 300
        assertEquals(600, cart.calculateTotal(), 0.01);
    }

    @Test
    void testJdbcConstruction() {
        CartItem ci = new CartItem(burger, 3);
        Cart loaded = new Cart("cart-id", customer, java.util.List.of(ci));
        assertEquals("cart-id", loaded.getId());
        assertEquals(1, loaded.getItems().size());
    }
}
