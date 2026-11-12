package com.tickethub.venue.dto;

import com.tickethub.venue.SeatCategory;

import java.time.Instant;
import java.util.List;

// One venue with its seat layout summarised per row
public record VenueDetailsResponse(
        Long id,
        String name,
        String city,
        String address,
        long totalSeats,
        Instant createdAt,
        List<RowResponse> rows
) {

    public record RowResponse(String label, SeatCategory category, int seatCount) {
    }
}