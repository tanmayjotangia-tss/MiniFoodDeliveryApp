package com.fooddeliveryapp.services.delivery;

import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FirstAvailableDeliveryAssignmentTest {

    private FirstAvailableDeliveryAssignment strategy;
    private Order order;

    @BeforeEach
    void setUp() {
        strategy = new FirstAvailableDeliveryAssignment();
        order = new Order("cust-1", "John");
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
    }

    @Test
    void testAssignsFirstAvailable() {
        DeliveryPartner busy = new DeliveryPartner("p1", "Busy",
                "b@test.com", "1111111111", "Pass@123", 5000, false, 5);
        DeliveryPartner available = new DeliveryPartner("p2", "Available",
                "a@test.com", "2222222222", "Pass@123", 5000, true, 5);

        DeliveryPartner result = strategy.assign(order, Arrays.asList(busy, available));
        assertNotNull(result);
        assertEquals("Available", result.getName());
    }

    @Test
    void testReturnsNullWhenNoneAvailable() {
        DeliveryPartner busy1 = new DeliveryPartner("p1", "Busy1",
                "b1@test.com", "1111111111", "Pass@123", 5000, false, 5);
        DeliveryPartner busy2 = new DeliveryPartner("p2", "Busy2",
                "b2@test.com", "2222222222", "Pass@123", 5000, false, 5);

        DeliveryPartner result = strategy.assign(order, Arrays.asList(busy1, busy2));
        assertNull(result);
    }

    @Test
    void testReturnsNullForEmptyList() {
        DeliveryPartner result = strategy.assign(order, Collections.emptyList());
        assertNull(result);
    }

    @Test
    void testSkipsBusyPartners() {
        DeliveryPartner busy = new DeliveryPartner("p1", "Busy",
                "b@test.com", "1111111111", "Pass@123", 5000, false, 5);
        DeliveryPartner free = new DeliveryPartner("p2", "Free",
                "f@test.com", "2222222222", "Pass@123", 5000, true, 5);
        DeliveryPartner alsoFree = new DeliveryPartner("p3", "AlsoFree",
                "af@test.com", "3333333333", "Pass@123", 5000, true, 5);

        DeliveryPartner result = strategy.assign(order, List.of(busy, free, alsoFree));

        // Should pick first available, which is "Free"
        assertEquals("Free", result.getName());
    }
}
