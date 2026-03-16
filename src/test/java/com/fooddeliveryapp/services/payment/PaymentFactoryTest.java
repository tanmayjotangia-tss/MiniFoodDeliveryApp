package com.fooddeliveryapp.services.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentFactoryTest {

    @Test
    void testCashPayment() {
        PaymentStrategy strategy = PaymentFactory.getStrategy("cash");
        assertNotNull(strategy);
        assertTrue(strategy instanceof CashPayment);
    }

    @Test
    void testCashPaymentCaseInsensitive() {
        PaymentStrategy strategy = PaymentFactory.getStrategy("CASH");
        assertTrue(strategy instanceof CashPayment);
    }

    @Test
    void testUpiPayment() {
        PaymentStrategy strategy = PaymentFactory.getStrategy("upi");
        assertNotNull(strategy);
        assertTrue(strategy instanceof UpiPayment);
    }

    @Test
    void testUpiPaymentCaseInsensitive() {
        PaymentStrategy strategy = PaymentFactory.getStrategy("UPI");
        assertTrue(strategy instanceof UpiPayment);
    }

    @Test
    void testUnsupportedModeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentFactory.getStrategy("bitcoin"));
    }

    @Test
    void testCashPaymentExecutes() {
        PaymentStrategy strategy = PaymentFactory.getStrategy("cash");
        assertDoesNotThrow(() -> strategy.pay(100));
    }

    @Test
    void testUpiPaymentExecutes() {
        PaymentStrategy strategy = PaymentFactory.getStrategy("upi");
        assertDoesNotThrow(() -> strategy.pay(200));
    }
}
