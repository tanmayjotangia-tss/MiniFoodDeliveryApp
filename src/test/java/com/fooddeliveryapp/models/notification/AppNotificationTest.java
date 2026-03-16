package com.fooddeliveryapp.models.notification;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AppNotificationTest {

    @Test
    void testConstruction() {
        AppNotification n = new AppNotification("Test message");
        assertNotNull(n.getId());
        assertEquals("Test message", n.getMessage());
        assertNotNull(n.getTimestamp());
        assertFalse(n.isRead());
    }

    @Test
    void testMarkAsRead() {
        AppNotification n = new AppNotification("Test");
        assertFalse(n.isRead());
        n.markAsRead();
        assertTrue(n.isRead());
    }

    @Test
    void testJdbcConstruction() {
        LocalDateTime ts = LocalDateTime.of(2025, 1, 15, 10, 30);
        AppNotification n = new AppNotification("notif-id", "Hello", ts, true);
        assertEquals("notif-id", n.getId());
        assertEquals("Hello", n.getMessage());
        assertEquals(ts, n.getTimestamp());
        assertTrue(n.isRead());
    }

    @Test
    void testFormattedTime() {
        LocalDateTime ts = LocalDateTime.of(2025, 3, 15, 14, 30);
        AppNotification n = new AppNotification("id", "msg", ts, false);
        assertEquals("15-03-2025 14:30", n.getFormattedTime());
    }

    @Test
    void testToStringNewNotification() {
        AppNotification n = new AppNotification("Test");
        String str = n.toString();
        assertTrue(str.startsWith("[NEW]"));
        assertTrue(str.contains("Test"));
    }

    @Test
    void testToStringReadNotification() {
        AppNotification n = new AppNotification("Test");
        n.markAsRead();
        assertTrue(n.toString().startsWith("[READ]"));
    }
}
