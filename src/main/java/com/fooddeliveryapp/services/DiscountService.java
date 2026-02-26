package com.fooddeliveryapp.services;

import com.fooddeliveryapp.services.discount.DiscountStrategy;
public class DiscountService {

    private DiscountStrategy currentStrategy;

    public DiscountService(DiscountStrategy strategy) {
        this.currentStrategy = strategy;
    }

    public void setDiscountStrategy(DiscountStrategy strategy) {
        this.currentStrategy = strategy;
    }

    public double calculateDiscount(double total) {
        if (currentStrategy == null) return 0;
        return currentStrategy.calculate(total);
    }
}