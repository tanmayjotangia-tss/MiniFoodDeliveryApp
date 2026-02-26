package com.fooddeliveryapp.services.discount;

public interface DiscountCondition {
    boolean isApplicable(double total);
}