package com.fooddeliveryapp.services.discount;

public class MinimumAmountCondition
        implements DiscountCondition {

    private final double minimum;

    public MinimumAmountCondition(double minimum) {
        this.minimum = minimum;
    }

    @Override
    public boolean isApplicable(double total) {
        return total >= minimum;
    }
}
