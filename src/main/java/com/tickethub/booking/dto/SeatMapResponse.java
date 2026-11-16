package com.tickethub.booking.dto;

import com.tickethub.booking.SeatStatus;
import com.tickethub.venue.SeatCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Everything the seat-selection page needs for one show
public record SeatMapResponse(
        Long showId,
        Long eventId,
        String eventTitle,
        String venueName,
        String city,
        Instant startTime,
        Instant endTime,
        List<Price> prices,
        List<Row> rows,
        int availableSeats
) {

    public record Price(SeatCategory category, BigDecimal price) {
    }

    public record Row(String label, List<SeatInfo> seats) {
    }

    public record SeatInfo(Long id, int number, SeatCategory category, SeatStatus status) {
    }
}