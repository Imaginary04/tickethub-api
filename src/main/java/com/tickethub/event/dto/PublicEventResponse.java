package com.tickethub.event.dto;

import com.tickethub.event.EventCategory;

import java.util.List;

// Public event page: no status, no organizer, only upcoming shows
public record PublicEventResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        String posterUrl,
        List<ShowResponse> shows
) {
}