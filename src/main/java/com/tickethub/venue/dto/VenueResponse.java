package com.tickethub.venue.dto;

import java.time.Instant;

// Venue without its seats: used for "create" and "my venues"
public record VenueResponse(
        Long id,
        String name,
        String city,
        String address,
        long totalSeats,
        Instant createdAt
) {
}