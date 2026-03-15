package com.fooddeliveryapp.services.payment;

public class PaymentFactory {

    public static PaymentStrategy getStrategy(String mode) {

        if (mode.equalsIgnoreCase("cash"))
            return new CashPayment();

        if (mode.equalsIgnoreCase("upi"))
            return new UpiPayment();

        throw new IllegalArgumentException("Unsupported payment mode");
    }
}
