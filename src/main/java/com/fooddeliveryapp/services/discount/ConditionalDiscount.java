package com.fooddeliveryapp.services.discount;

public class ConditionalDiscount
        implements DiscountStrategy {

    private final DiscountStrategy discountStrategy;
    private final DiscountCondition condition;

    public ConditionalDiscount(
            DiscountStrategy discountStrategy,
            DiscountCondition condition) {

        this.discountStrategy = discountStrategy;
        this.condition = condition;
    }

    @Override
    public double calculate(double total) {

        if (condition.isApplicable(total)) {
            return discountStrategy.calculate(total);
        }

        return 0;
    }
}
