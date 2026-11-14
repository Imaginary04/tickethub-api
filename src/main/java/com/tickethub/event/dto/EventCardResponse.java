package com.tickethub.event.dto;

import com.tickethub.event.EventCategory;

import java.math.BigDecimal;
import java.time.Instant;

// One event in the public listing: what a card on the events page shows
public record EventCardResponse(
        Long id,
        String title,
        EventCategory category,
        String posterUrl,
        Instant nextShowTime,
        BigDecimal minPrice
) {
}