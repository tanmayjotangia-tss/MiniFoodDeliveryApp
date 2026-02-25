package com.fooddeliveryapp.services.discount;

public class AmountDiscount implements DiscountStrategy {

    private final double threshold;
    private final double percentage;

    public AmountDiscount(double threshold, double percentage) {
        this.threshold = threshold;
        this.percentage = percentage;
    }

    @Override
    public double calculate(double total) {
        if (total > threshold)
            return total * (percentage / 100);
        return 0;
    }
}
