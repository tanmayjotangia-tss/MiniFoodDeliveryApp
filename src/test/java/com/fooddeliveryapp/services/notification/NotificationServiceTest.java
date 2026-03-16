package com.fooddeliveryapp.services.notification;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.repository.DBNotificationRepository;
import com.fooddeliveryapp.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private Repository<User> userRepository;
    @Mock private DBNotificationRepository notificationRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(userRepository, notificationRepository);
    }

    @Test
    void testNotifyUser() {
        Customer customer = new Customer("John", "john@test.com", "9876543210",
                "Addr", "Pass@123", EnumSet.noneOf(NotificationType.class));
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        service.notifyUser(customer.getId(), "Your order is confirmed!");

        verify(notificationRepository).insert(eq(customer.getId()), any());
    }

    @Test
    void testNotifyUserThrowsWhenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.notifyUser("missing", "Hello!"));
    }
}
