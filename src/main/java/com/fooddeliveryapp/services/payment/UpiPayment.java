package com.fooddeliveryapp.services.payment;

public class UpiPayment implements PaymentStrategy {

    @Override
    public void pay(double amount) {
        System.out.println("Processing UPI payment of ₹" + amount);
    }
}
