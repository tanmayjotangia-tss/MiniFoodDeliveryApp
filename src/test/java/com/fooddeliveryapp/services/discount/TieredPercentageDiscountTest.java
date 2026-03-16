package com.fooddeliveryapp.services.discount;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TieredPercentageDiscountTest {

    private TieredPercentageDiscount discount;

    @BeforeEach
    void setUp() {
        discount = new TieredPercentageDiscount();
    }

    @Test
    void testNoSlabReturnsZero() {
        assertEquals(0, discount.calculate(1000), 0.01);
    }

    @Test
    void testAddSlabAndCalculate() {
        discount.addSlab(500, 10);
        assertEquals(80, discount.calculate(800), 0.01); // 800 * 10% = 80
    }

    @Test
    void testBelowThresholdReturnsZero() {
        discount.addSlab(500, 10);
        assertEquals(0, discount.calculate(300), 0.01);
    }

    @Test
    void testMultipleSlabsHighestMatches() {
        discount.addSlab(200, 5);
        discount.addSlab(500, 10);
        discount.addSlab(1000, 20);

        assertEquals(15, discount.calculate(300), 0.01);  // 300 * 5%
        assertEquals(60, discount.calculate(600), 0.01);  // 600 * 10%
        assertEquals(300, discount.calculate(1500), 0.01); // 1500 * 20%
    }

    @Test
    void testExactThreshold() {
        discount.addSlab(500, 10);
        assertEquals(50, discount.calculate(500), 0.01); // 500 * 10%
    }

    @Test
    void testRemoveSlab() {
        discount.addSlab(500, 10);
        discount.removeSlab(500);
        assertEquals(0, discount.calculate(800), 0.01);
    }

    @Test
    void testGetSlabs() {
        discount.addSlab(500, 10);
        discount.addSlab(1000, 20);
        assertEquals(2, discount.getSlabs().size());
    }
}
