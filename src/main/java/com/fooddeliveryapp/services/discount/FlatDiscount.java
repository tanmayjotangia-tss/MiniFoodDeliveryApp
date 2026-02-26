package com.fooddeliveryapp.services.discount;

public class FlatDiscount implements DiscountStrategy {

    private final double amount;

    public FlatDiscount(double amount) {
        this.amount = amount;
    }

    @Override
    public double calculate(double total) {
        return Math.min(amount, total);
    }
}
