package com.fooddeliveryapp.models.order;

import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.services.notification.OrderObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order("customer-1", "John Doe");
    }

    @Test
    void testConstruction() {
        assertNotNull(order.getId());
        assertEquals("customer-1", order.getCustomerId());
        assertEquals("John Doe", order.getCustomerName());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void testRejectsBlankCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> new Order("", "name"));
    }

    @Test
    void testRejectsNullCustomerId() {
        assertThrows(IllegalArgumentException.class, () -> new Order(null, "name"));
    }

    @Test
    void testAddItem() {
        MenuItem item = new MenuItem("Burger", 150);
        order.addItem(new OrderItem(item, 2));
        assertEquals(1, order.getItems().size());
    }

    @Test
    void testTotalAmount() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 2));
        order.addItem(new OrderItem(new MenuItem("Pizza", 300), 1));
        assertEquals(600, order.getTotalAmount(), 0.01);
    }

    @Test
    void testFinalAmountWithDiscount() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 2)); // 300
        order.applyDiscount(50);
        assertEquals(250, order.getFinalAmount(), 0.01);
    }

    @Test
    void testFinalAmountNeverNegative() {
        order.addItem(new OrderItem(new MenuItem("Burger", 100), 1));
        order.applyDiscount(200);
        assertEquals(0, order.getFinalAmount(), 0.01);
    }

    @Test
    void testMarkPaid() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(PaymentMode.CASH, order.getPaymentMode());
    }

    @Test
    void testMarkPaidRejectsEmptyOrder() {
        assertThrows(IllegalStateException.class, () -> order.markPaid(PaymentMode.CASH));
    }

    @Test
    void testConfirmByAdmin() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.confirmByAdmin();
        assertEquals(OrderStatus.CONFIRMED_BY_ADMIN, order.getStatus());
    }

    @Test
    void testConfirmByAdminRejectsNonPaid() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        assertThrows(IllegalStateException.class, () -> order.confirmByAdmin());
    }

    @Test
    void testAssignDeliveryPartner() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.UPI);
        order.confirmByAdmin();
        order.assignDeliveryPartner("partner-1");
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());
        assertEquals("partner-1", order.getDeliveryPartnerId());
    }

    @Test
    void testAssignRejectsNonConfirmed() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        assertThrows(IllegalStateException.class, () -> order.assignDeliveryPartner("p1"));
    }

    @Test
    void testMarkDelivered() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.confirmByAdmin();
        order.assignDeliveryPartner("partner-1");
        order.markDelivered();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void testMarkDeliveredRejectsNonOutForDelivery() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        assertThrows(IllegalStateException.class, () -> order.markDelivered());
    }

    @Test
    void testCancel() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void testCancelRejectsDelivered() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.confirmByAdmin();
        order.assignDeliveryPartner("p1");
        order.markDelivered();
        assertThrows(IllegalStateException.class, () -> order.cancel());
    }

    @Test
    void testCancelRejectsAlreadyCancelled() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.cancel();
        assertThrows(IllegalStateException.class, () -> order.cancel());
    }

    @Test
    void testCancelRejectsOutForDelivery() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.confirmByAdmin();
        order.assignDeliveryPartner("p1");
        assertThrows(IllegalStateException.class, () -> order.cancel());
    }

    @Test
    void testCancelByAdmin() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        order.confirmByAdmin();
        order.assignDeliveryPartner("p1");
        order.cancelByAdmin();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void testCannotModifyAfterPayment() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);
        assertThrows(IllegalStateException.class,
                () -> order.addItem(new OrderItem(new MenuItem("Pizza", 200), 1)));
    }

    @Test
    void testObserverNotification() {
        List<String> messages = new ArrayList<>();
        OrderObserver observer = (o, msg) -> messages.add(msg);

        order.addObserver(observer);
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("Order placed"));
    }

    @Test
    void testClearObservers() {
        List<String> messages = new ArrayList<>();
        OrderObserver observer = (o, msg) -> messages.add(msg);

        order.addObserver(observer);
        order.clearObservers();
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        order.markPaid(PaymentMode.CASH);

        assertTrue(messages.isEmpty());
    }

    @Test
    void testGetItemsReturnsUnmodifiable() {
        order.addItem(new OrderItem(new MenuItem("Burger", 150), 1));
        assertThrows(UnsupportedOperationException.class,
                () -> order.getItems().clear());
    }
}
