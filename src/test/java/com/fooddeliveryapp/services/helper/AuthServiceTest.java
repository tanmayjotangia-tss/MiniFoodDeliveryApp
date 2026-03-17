package com.fooddeliveryapp.services.helper;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.repository.Repository;
import com.fooddeliveryapp.services.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private Repository<User> userRepository;
    @Mock private OrderService orderService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, orderService);
    }

    @Test
    void testRegisterCustomer() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        boolean result = authService.registerCustomer("John", "john@test.com",
                "9876543210", "123 Main St", "Pass@123",
                EnumSet.of(NotificationType.EMAIL));

        assertTrue(result);
        verify(userRepository).save(any(Customer.class));
    }

    @Test
    void testRegisterRejectsDuplicateEmail() {
        Customer existing = new Customer("Existing", "john@test.com", "1111111111",
                "Addr", "Pass@123", EnumSet.noneOf(NotificationType.class));
        when(userRepository.findAll()).thenReturn(List.of(existing));

        boolean result = authService.registerCustomer("John", "john@test.com",
                "9876543210", "123 Main St", "Pass@123",
                EnumSet.of(NotificationType.EMAIL));

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterRejectsDuplicatePhone() {
        Customer existing = new Customer("Existing", "other@test.com", "9876543210",
                "Addr", "Pass@123", EnumSet.noneOf(NotificationType.class));
        when(userRepository.findAll()).thenReturn(List.of(existing));

        boolean result = authService.registerCustomer("John", "john@test.com",
                "9876543210", "123 Main St", "Pass@123",
                EnumSet.of(NotificationType.EMAIL));

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterDeliveryPartner() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        boolean result = authService.registerDeliveryPartner("Bob", "bob@test.com",
                "1111111111", "Pass@123");

        assertTrue(result);
        verify(userRepository).save(any(DeliveryPartner.class));
        verify(orderService).tryAssignWaitingOrdersToPartner(any(DeliveryPartner.class));
    }

    @Test
    void testLogin() {
        Customer customer = new Customer("John", "john@test.com", "9876543210",
                "Addr", "Pass@123", EnumSet.noneOf(NotificationType.class));
        when(userRepository.findAll()).thenReturn(List.of(customer));

        User loggedIn = authService.login("john@test.com", "Pass@123");
        assertEquals(customer, loggedIn);
    }

    @Test
    void testLoginThrowsWhenNotFound() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        assertThrows(EntityNotFoundException.class,
                () -> authService.login("nobody@test.com", "Pass@123"));
    }

    @Test
    void testLoginThrowsWithWrongPassword() {
        Customer customer = new Customer("John", "john@test.com", "9876543210",
                "Addr", "Pass@123", EnumSet.noneOf(NotificationType.class));
        when(userRepository.findAll()).thenReturn(List.of(customer));

        assertThrows(EntityNotFoundException.class,
                () -> authService.login("john@test.com", "WrongPass@1"));
    }
}
