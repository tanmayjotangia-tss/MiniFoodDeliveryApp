package com.fooddeliveryapp.models.users;

import com.fooddeliveryapp.models.notification.AppNotification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPartnerTest {

    private DeliveryPartner createPartner() {
        return new DeliveryPartner("Alice", "alice@test.com", "9876543210", "Pass@123", 5000);
    }

    @Test
    void testConstruction() {
        DeliveryPartner p = createPartner();
        assertNotNull(p.getId());
        assertEquals("Alice", p.getName());
        assertTrue(p.isAvailable());
        assertEquals(5000, p.getBasicPay());
        assertEquals(5, p.getIncentivePercentage());
    }

    @Test
    void testRole() {
        assertEquals(Role.DELIVERY_PARTNER, createPartner().getRole());
    }

    @Test
    void testAvailability() {
        DeliveryPartner p = createPartner();
        assertTrue(p.isAvailable());
        p.setAvailable(false);
        assertFalse(p.isAvailable());
    }

    @Test
    void testUpdateBasicPay() {
        DeliveryPartner p = createPartner();
        p.updateBasicPay(7000);
        assertEquals(7000, p.getBasicPay());
    }

    @Test
    void testUpdateBasicPayRejectsNegative() {
        DeliveryPartner p = createPartner();
        assertThrows(IllegalArgumentException.class, () -> p.updateBasicPay(-100));
    }

    @Test
    void testUpdateBasicPayRejectsZero() {
        DeliveryPartner p = createPartner();
        assertThrows(IllegalArgumentException.class, () -> p.updateBasicPay(0));
    }

    @Test
    void testUpdateIncentivePercentage() {
        DeliveryPartner p = createPartner();
        p.updateIncentivePercentage(10);
        assertEquals(10, p.getIncentivePercentage());
    }

    @Test
    void testUpdateIncentiveRejectsNegative() {
        DeliveryPartner p = createPartner();
        assertThrows(IllegalArgumentException.class, () -> p.updateIncentivePercentage(-5));
    }

    @Test
    void testInheritsUserNotifications() {
        DeliveryPartner p = createPartner();
        assertTrue(p.getNotifications().isEmpty());
        p.addNotification("Order assigned");
        assertEquals(1, p.getNotifications().size());
    }

    @Test
    void testRestoreNotifications() {
        DeliveryPartner p = createPartner();
        AppNotification n1 = new AppNotification("msg1");
        AppNotification n2 = new AppNotification("msg2");
        p.restoreNotifications(List.of(n1, n2));
        assertEquals(2, p.getNotifications().size());
    }

    @Test
    void testJdbcConstruction() {
        DeliveryPartner p = new DeliveryPartner("fixed-id", "Alice", "alice@test.com",
                "9876543210", "Pass@123", 6000, false, 8);
        assertEquals("fixed-id", p.getId());
        assertEquals(6000, p.getBasicPay());
        assertFalse(p.isAvailable());
        assertEquals(8, p.getIncentivePercentage());
    }
}
