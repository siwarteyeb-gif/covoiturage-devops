package com.example.covoiturage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** RG8 : partage des frais. prix par passager = distance_km × tarif_par_km ÷ (passagers + 1). */
@Service
public class PricingService {

    private final BigDecimal ratePerKm;
    private final BigDecimal maxFactor;

    public PricingService(@Value("${pricing.rate-per-km:0.30}") double ratePerKm,
                          @Value("${pricing.max-factor:1.5}") double maxFactor) {
        this.ratePerKm = BigDecimal.valueOf(ratePerKm);
        this.maxFactor = BigDecimal.valueOf(maxFactor);
    }

    /** Prix suggéré par passager (TND), en supposant que toutes les places proposées sont occupées. */
    public BigDecimal suggestedPrice(double distanceKm, int passengers) {
        BigDecimal totalCost = BigDecimal.valueOf(distanceKm).multiply(ratePerKm);
        return totalCost.divide(BigDecimal.valueOf(passengers + 1L), 2, RoundingMode.HALF_UP);
    }

    /** Prix maximum que le conducteur peut demander (plafond pour éviter tout profit). */
    public BigDecimal maxPrice(double distanceKm, int passengers) {
        BigDecimal max = suggestedPrice(distanceKm, passengers).multiply(maxFactor).setScale(2, RoundingMode.HALF_UP);
        return max.max(BigDecimal.ONE);   // au moins 1 TND, même pour un très court trajet
    }
}
