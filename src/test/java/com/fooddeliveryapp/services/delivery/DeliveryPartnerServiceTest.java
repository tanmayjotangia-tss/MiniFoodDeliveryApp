package com.fooddeliveryapp.services.delivery;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.repository.UserRepository;
import com.fooddeliveryapp.services.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryPartnerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrderService orderService;

    private DeliveryPartnerService service;
    private DeliveryPartner partner;

    @BeforeEach
    void setUp() {
        service = new DeliveryPartnerService(userRepository, orderService);
        partner = new DeliveryPartner("partner-1", "Bob", "bob@test.com",
                "1111111111", "Pass@123", 5000, true, 10);
    }

    @Test
    void testGetAllPartners() {
        Customer customer = new Customer("John", "john@test.com", "9876543210",
                "Addr", "Pass@123", EnumSet.noneOf(NotificationType.class));
        when(userRepository.findAll()).thenReturn(List.of(customer, partner));

        List<DeliveryPartner> result = service.getAllPartners();
        assertEquals(1, result.size());
        assertEquals("Bob", result.get(0).getName());
    }

    @Test
    void testFindById() {
        when(userRepository.findAll()).thenReturn(List.of(partner));

        DeliveryPartner found = service.findById("partner-1");
        assertEquals("Bob", found.getName());
    }

    @Test
    void testFindByIdThrows() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        assertThrows(EntityNotFoundException.class,
                () -> service.findById("nonexistent"));
    }

    @Test
    void testUpdateBasicPay() {
        when(userRepository.findById("partner-1")).thenReturn(Optional.of(partner));

        service.updateBasicPay("partner-1", 7000);

        assertEquals(7000, partner.getBasicPay());
        verify(userRepository).save(partner);
    }

    @Test
    void testUpdateBasicPayThrowsForMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> service.updateBasicPay("missing", 7000));
    }

    @Test
    void testUpdateIncentivePercentage() {
        when(userRepository.findAll()).thenReturn(List.of(partner));

        service.updateIncentivePercentage("partner-1", 15);

        assertEquals(15, partner.getIncentivePercentage());
        verify(userRepository).save(partner);
    }

    @Test
    void testRemovePartner() {
        service.removePartner("partner-1");
        verify(userRepository).delete("partner-1");
    }

    @Test
    void testCalculateEarnings() {
        when(userRepository.findAll()).thenReturn(List.of(partner));

        MenuItem burger = new MenuItem("Burger", 100);
        Order delivered = new Order("cust-1", "John");
        delivered.addItem(new OrderItem(burger, 2));
        delivered.markPaid(PaymentMode.CASH);
        delivered.confirmByAdmin();
        delivered.assignDeliveryPartner("partner-1");
        delivered.markDelivered();

        when(orderService.getOrdersByPartner("partner-1"))
                .thenReturn(List.of(delivered));

        double earnings = service.calculateEarnings("partner-1");

        // basicPay (5000) + incentive (200 * 10/100 = 20) = 5020
        assertEquals(5020, earnings, 0.01);
    }

    @Test
    void testCalculateEarningsNoDeliveries() {
        when(userRepository.findAll()).thenReturn(List.of(partner));
        when(orderService.getOrdersByPartner("partner-1"))
                .thenReturn(Collections.emptyList());

        double earnings = service.calculateEarnings("partner-1");
        assertEquals(5000, earnings, 0.01); 
    }
}
