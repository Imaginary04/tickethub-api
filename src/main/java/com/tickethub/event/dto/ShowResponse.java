package com.tickethub.event.dto;

import com.tickethub.event.Show;
import com.tickethub.event.ShowPricing;
import com.tickethub.venue.SeatCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ShowResponse(
        Long id,
        Long venueId,
        String venueName,
        String city,
        Instant startTime,
        Instant endTime,
        List<PriceResponse> prices
) {

    public record PriceResponse(SeatCategory category, BigDecimal price) {
    }

    // Needs show.venue and show.prices loaded (inside a transaction or via @EntityGraph)
    public static ShowResponse from(Show show) {
        List<PriceResponse> prices = show.getPrices().stream()
                .sorted(Comparator.comparing(ShowPricing::getSeatCategory)) // SILVER, GOLD, PLATINUM
                .map(p -> new PriceResponse(p.getSeatCategory(), p.getPrice()))
                .toList();
        return new ShowResponse(
                show.getId(),
                show.getVenue().getId(),
                show.getVenue().getName(),
                show.getVenue().getCity(),
                show.getStartTime(),
                show.getEndTime(),
                prices);
    }
}