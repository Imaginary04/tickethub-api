package com.tickethub.event.dto;

import com.tickethub.event.EventCategory;
import com.tickethub.event.EventStatus;

import java.time.Instant;
import java.util.List;

// One event with all its shows (organizer view)
public record EventDetailsResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        String posterUrl,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<ShowResponse> shows
) {
}