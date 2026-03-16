package com.fooddeliveryapp.services.discount;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    private DiscountService service;

    @BeforeEach
    void setUp() {
        TieredPercentageDiscount tieredDiscount = new TieredPercentageDiscount();
        tieredDiscount.addSlab(500, 10);
        tieredDiscount.addSlab(1000, 20);
        service = new DiscountService(tieredDiscount);
    }

    @Test
    void testCalculateDiscount() {
        double discount = service.calculateDiscount(800);
        // 800 >= 500 threshold → 10% = 80
        assertEquals(80, discount, 0.01);
    }

    @Test
    void testCalculateDiscountAboveHigherSlab() {
        double discount = service.calculateDiscount(1500);
        // 1500 >= 1000 threshold → 20% = 300
        assertEquals(300, discount, 0.01);
    }

    @Test
    void testCalculateDiscountBelowThreshold() {
        double discount = service.calculateDiscount(300);
        assertEquals(0, discount, 0.01);
    }

    @Test
    void testGetTieredDiscount() {
        assertNotNull(service.getTieredDiscount());
    }
}
