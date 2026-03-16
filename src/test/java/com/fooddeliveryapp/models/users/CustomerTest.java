package com.fooddeliveryapp.models.users;

import com.fooddeliveryapp.models.notification.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    private Customer createCustomer() {
        return new Customer("John", "john@test.com", "9876543210",
                "123 Main St", "Pass@123", EnumSet.of(NotificationType.EMAIL));
    }

    @Test
    void testConstruction() {
        Customer c = createCustomer();
        assertNotNull(c.getId());
        assertEquals("John", c.getName());
        assertEquals("john@test.com", c.getEmail());
        assertEquals("9876543210", c.getPhone());
        assertEquals("123 Main St", c.getAddress());
    }

    @Test
    void testRole() {
        assertEquals(Role.CUSTOMER, createCustomer().getRole());
    }

    @Test
    void testNotificationPreferences() {
        Customer c = createCustomer();
        Set<NotificationType> prefs = c.getNotificationPreferences();
        assertTrue(prefs.contains(NotificationType.EMAIL));
        assertFalse(prefs.contains(NotificationType.PHONE));
    }

    @Test
    void testJdbcConstruction() {
        Customer c = new Customer("fixed-id", "John", "john@test.com", "9876543210",
                "123 Main St", "Pass@123", EnumSet.noneOf(NotificationType.class));
        assertEquals("fixed-id", c.getId());
    }

    @Test
    void testInheritsUserNotifications() {
        Customer c = createCustomer();
        assertTrue(c.getNotifications().isEmpty());
        c.addNotification("Test message");
        assertEquals(1, c.getNotifications().size());
    }
}
