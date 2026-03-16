package com.fooddeliveryapp.models.users;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    @Test
    void testConstruction() {
        Admin admin = new Admin("Admin", "admin@test.com", "1234567890", "Pass@123");
        assertNotNull(admin.getId());
        assertEquals("Admin", admin.getName());
        assertEquals("admin@test.com", admin.getEmail());
        assertEquals("1234567890", admin.getPhone());
        assertEquals("Pass@123", admin.getPassword());
    }

    @Test
    void testRole() {
        Admin admin = new Admin("Admin", "admin@test.com", "1234567890", "Pass@123");
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void testJdbcConstruction() {
        Admin admin = new Admin("fixed-id", "Admin", "admin@test.com", "1234567890", "Pass@123");
        assertEquals("fixed-id", admin.getId());
        assertEquals("Admin", admin.getName());
    }

    @Test
    void testRejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Admin("", "a@b.com", "1234567890", "Pass@123"));
    }

    @Test
    void testRejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new Admin("Name", "", "1234567890", "Pass@123"));
    }

    @Test
    void testRejectsBlankPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> new Admin("Name", "a@b.com", "1234567890", ""));
    }
}
