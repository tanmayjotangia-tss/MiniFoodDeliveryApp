package com.fooddeliveryapp.services;

import com.fooddeliveryapp.services.discount.DiscountStrategy;

public class DiscountService {

    private DiscountStrategy currentStrategy;

    public DiscountService(DiscountStrategy strategy) {
        this.currentStrategy = strategy;
    }

    public void updateStrategy(DiscountStrategy strategy) {
        this.currentStrategy = strategy;
    }

    public DiscountStrategy getCurrentStrategy() {
        return currentStrategy;
    }
}