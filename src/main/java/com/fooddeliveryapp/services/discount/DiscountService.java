package com.fooddeliveryapp.services.discount;

public class DiscountService {

    private final TieredPercentageDiscount tieredDiscount;

    public DiscountService(TieredPercentageDiscount tieredDiscount) {
        this.tieredDiscount = tieredDiscount;
    }

    public double calculateDiscount(double total) {
        return tieredDiscount.calculate(total);
    }

    public TieredPercentageDiscount getTieredDiscount() {
        return tieredDiscount;
    }
}