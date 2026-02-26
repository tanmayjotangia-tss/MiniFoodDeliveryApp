package com.fooddeliveryapp.services.discount;

import java.util.List;

public class CompositeDiscount
        implements DiscountStrategy {

    private final List<DiscountStrategy> discounts;

    public CompositeDiscount(List<DiscountStrategy> discounts) {
        this.discounts = discounts;
    }

    @Override
    public double calculate(double total) {

        double totalDiscount = 0;

        for (DiscountStrategy discount : discounts) {
            totalDiscount += discount.calculate(total);
        }

        return totalDiscount;
    }
}
