package com.example.covoiturage.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private final PricingService pricing = new PricingService(0.30, 1.5);

    @Test
    void suggestedPriceSharesTheCostBetweenPassengersAndDriver() {
        // 12 km x 0,30 = 3,6 TND ; 3 passagers + le conducteur = 4 parts
        assertEquals(new BigDecimal("0.90"), pricing.suggestedPrice(12.0, 3));
    }

    @Test
    void maxPriceIsSuggestedPriceTimesFactor() {
        assertEquals(new BigDecimal("1.35"), pricing.maxPrice(12.0, 3));
    }

    @Test
    void maxPriceIsAtLeastOneDinar() {
        assertEquals(new BigDecimal("1.00"), pricing.maxPrice(1.0, 3));
    }
}
