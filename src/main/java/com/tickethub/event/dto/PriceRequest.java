package com.tickethub.event.dto;

import com.tickethub.venue.SeatCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// { "category": "GOLD", "price": 2499.50 }
public record PriceRequest(
        @NotNull SeatCategory category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price
) {
}