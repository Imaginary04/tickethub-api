package com.tickethub.event.dto;

import com.tickethub.event.Event;
import com.tickethub.event.EventCategory;
import com.tickethub.event.EventStatus;

import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        String posterUrl,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getPosterUrl(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}