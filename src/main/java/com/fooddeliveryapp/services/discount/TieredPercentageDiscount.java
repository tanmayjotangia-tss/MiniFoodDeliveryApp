package com.fooddeliveryapp.services.discount;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TieredPercentageDiscount implements DiscountStrategy, Serializable {

    private static final long serialVersionUID = 1L;

    private final NavigableMap<Double, Double> slabs = new TreeMap<>();

    public void addSlab(double threshold, double percentage) {

        if (threshold <= 0 || percentage <= 0)
            throw new IllegalArgumentException("Threshold and percentage must be positive.");

        if (percentage > 100) throw new IllegalArgumentException("Percentage cannot exceed 100.");

        slabs.put(threshold, percentage);
    }

    public void removeSlab(double threshold) {
        slabs.remove(threshold);
    }

    public Map<Double, Double> getSlabs() {
        return Collections.unmodifiableMap(slabs);
    }

    @Override
    public double calculate(double total) {

        if (slabs.isEmpty()) return 0;

        Map.Entry<Double, Double> applicable = slabs.floorEntry(total);

        if (applicable == null) return 0;

        return total * (applicable.getValue() / 100);
    }
}