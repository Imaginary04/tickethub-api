package com.tickethub.event;

import com.tickethub.common.dto.PageResponse;
import com.tickethub.event.dto.EventCardResponse;
import com.tickethub.event.dto.PublicEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Public: GET /api/events/** is open to everyone in SecurityConfig
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final PublicEventService publicEventService;

    // e.g. /api/events?city=Mumbai&date=2026-12-20&category=CONCERT&q=rock&page=0&size=12
    @GetMapping
    public PageResponse<EventCardResponse> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return publicEventService.search(city, date, category, query, page, size);
    }

    @GetMapping("/{id}")
    public PublicEventResponse get(@PathVariable Long id) {
        return publicEventService.get(id);
    }
}