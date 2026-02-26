package com.fooddeliveryapp.services.discount;

public class PercentageDiscount implements DiscountStrategy {

    private final double percentage;

    public PercentageDiscount(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public double calculate(double total) {
        return total * (percentage / 100);
    }
}
