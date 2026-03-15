package com.fooddeliveryapp.services.payment;

public class CashPayment implements PaymentStrategy {

    @Override
    public void pay(double amount) {
        System.out.println("Cash payment received: ₹" + amount);
    }
}
