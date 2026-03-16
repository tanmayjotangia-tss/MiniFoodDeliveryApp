package com.fooddeliveryapp.services.order;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.order.PaymentMode;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.User;
import com.fooddeliveryapp.repository.Repository;
import com.fooddeliveryapp.services.delivery.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.discount.DiscountService;
import com.fooddeliveryapp.services.notification.NotificationService;
import com.fooddeliveryapp.services.payment.PaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private Repository<Order> orderRepository;
    @Mock private DiscountService discountService;
    @Mock private Repository<User> userRepository;
    @Mock private DeliveryAssignmentStrategy deliveryStrategy;
    @Mock private NotificationService notificationService;
    @Mock private PaymentStrategy paymentStrategy;

    private OrderService orderService;
    private Customer customer;
    private Cart cart;
    private MenuItem burger;

    @BeforeEach
    void setUp() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        orderService = new OrderService(orderRepository, discountService,
                userRepository, deliveryStrategy, notificationService);

        customer = new Customer("John", "john@test.com", "9876543210",
                "123 Main St", "Pass@123", EnumSet.noneOf(NotificationType.class));
        cart = new Cart(customer);
        burger = new MenuItem("Burger", 150);
    }

    @Test
    void testCheckoutCart() {
        cart.addItem(burger, 2);
        when(discountService.calculateDiscount(300)).thenReturn(0.0);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        Order order = orderService.checkoutCart(cart, paymentStrategy, PaymentMode.CASH);

        assertNotNull(order);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(300, order.getTotalAmount(), 0.01);
        verify(paymentStrategy).pay(300);
        verify(orderRepository).save(order);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testCheckoutWithDiscount() {
        cart.addItem(burger, 2);
        when(discountService.calculateDiscount(300)).thenReturn(30.0);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        Order order = orderService.checkoutCart(cart, paymentStrategy, PaymentMode.UPI);

        assertEquals(270, order.getFinalAmount(), 0.01);
        verify(paymentStrategy).pay(270);
    }

    @Test
    void testCheckoutEmptyCartThrows() {
        assertThrows(InvalidOperationException.class,
                () -> orderService.checkoutCart(cart, paymentStrategy, PaymentMode.CASH));
    }

    @Test
    void testConfirmOrderAssignsPartner() {
        cart.addItem(burger, 2);
        when(discountService.calculateDiscount(anyDouble())).thenReturn(0.0);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        Order order = orderService.checkoutCart(cart, paymentStrategy, PaymentMode.CASH);

        DeliveryPartner partner = new DeliveryPartner("Bob", "bob@test.com",
                "1111111111", "Pass@123", 5000);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findAll()).thenReturn(List.of(customer, partner));
        when(deliveryStrategy.assign(any(), any())).thenReturn(partner);

        orderService.confirmOrder(order.getId());

        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());
        assertFalse(partner.isAvailable());
    }

    @Test
    void testConfirmOrderQueuesWhenNoPartners() {
        cart.addItem(burger, 2);
        when(discountService.calculateDiscount(anyDouble())).thenReturn(0.0);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        Order order = orderService.checkoutCart(cart, paymentStrategy, PaymentMode.CASH);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findAll()).thenReturn(List.of(customer));

        orderService.confirmOrder(order.getId());

        assertEquals(OrderStatus.CONFIRMED_BY_ADMIN, order.getStatus());
    }

    @Test
    void testConfirmOrderNotFoundThrows() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> orderService.confirmOrder("missing"));
    }

    @Test
    void testDeliverOrder() {
        cart.addItem(burger, 1);
        when(discountService.calculateDiscount(anyDouble())).thenReturn(0.0);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        Order order = orderService.checkoutCart(cart, paymentStrategy, PaymentMode.CASH);

        DeliveryPartner partner = new DeliveryPartner("Bob", "bob@test.com",
                "1111111111", "Pass@123", 5000);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findAll()).thenReturn(List.of(customer, partner));
        when(deliveryStrategy.assign(any(), any())).thenReturn(partner);

        orderService.confirmOrder(order.getId());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());

        when(userRepository.findById(partner.getId())).thenReturn(Optional.of(partner));
        orderService.deliverOrder(order.getId(), partner.getId());

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertTrue(partner.isAvailable());
    }

    @Test
    void testGetOrdersByCustomer() {
        Order o1 = new Order(customer.getId(), "John");
        Order o2 = new Order("other-customer", "Jane");
        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        List<Order> result = orderService.getOrdersByCustomer(customer.getId());
        assertEquals(1, result.size());
        assertEquals(customer.getId(), result.get(0).getCustomerId());
    }

    @Test
    void testCalculateTotalRevenue() {
        Order delivered = new Order(customer.getId(), "John");
        delivered.addItem(new com.fooddeliveryapp.models.order.OrderItem(burger, 2));
        delivered.markPaid(PaymentMode.CASH);
        delivered.confirmByAdmin();
        delivered.assignDeliveryPartner("partner-1");
        delivered.markDelivered();

        Order pending = new Order(customer.getId(), "John");
        pending.addItem(new com.fooddeliveryapp.models.order.OrderItem(burger, 1));
        pending.markPaid(PaymentMode.CASH);

        when(orderRepository.findAll()).thenReturn(List.of(delivered, pending));

        double revenue = orderService.calculateTotalRevenue();
        assertEquals(300, revenue, 0.01);
    }

    @Test
    void testGetTotalOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(
                new Order(customer.getId(), "John"),
                new Order(customer.getId(), "John")
        ));
        assertEquals(2, orderService.getTotalOrders());
    }
}
