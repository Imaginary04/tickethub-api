package com.tickethub.event;

import com.tickethub.common.dto.PageResponse;
import com.tickethub.common.exception.ResourceNotFoundException;
import com.tickethub.event.dto.EventCardResponse;
import com.tickethub.event.dto.PublicEventResponse;
import com.tickethub.event.dto.ShowResponse;
import com.tickethub.event.dto.ShowSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Read-only API for customers: no login needed, only PUBLISHED events, only upcoming shows
@Service
@RequiredArgsConstructor
public class PublicEventService {

    // A "date" filter means a calendar day in India, not in UTC
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T00:00:00Z");
    private static final int MAX_PAGE_SIZE = 50;

    private final EventRepository eventRepository;
    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public PageResponse<EventCardResponse> search(String city, LocalDate date, EventCategory category,
                                                  String query, int page, int size) {
        // Time window: from now (never past shows), or the chosen day
        Instant now = Instant.now();
        Instant from = now;
        Instant to = FAR_FUTURE;
        if (date != null) {
            Instant dayStart = date.atStartOfDay(ZONE).toInstant();
            from = dayStart.isAfter(now) ? dayStart : now;
            to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
        }
        String cityKey = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);

        // Build the WHERE clause from only the filters that were given
        List<Specification<Event>> filters = new ArrayList<>();
        filters.add(EventSpecifications.isPublished());
        filters.add(EventSpecifications.hasShowBetween(from, to, cityKey));
        if (category != null) {
            filters.add(EventSpecifications.hasCategory(category));
        }
        if (query != null && !query.isBlank()) {
            filters.add(EventSpecifications.titleContains(query));
        }

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<Event> events = eventRepository.findAll(Specification.allOf(filters), pageRequest);

        // One extra query for the whole page (not one per event): next show + lowest price
        Map<Long, ShowSummary> summaries = events.isEmpty()
                ? Map.of()
                : showRepository.summarize(events.map(Event::getId).toList(), from, to, cityKey).stream()
                .collect(Collectors.toMap(ShowSummary::eventId, Function.identity()));

        return PageResponse.from(events.map(event -> {
            ShowSummary summary = summaries.get(event.getId());
            return new EventCardResponse(event.getId(), event.getTitle(), event.getCategory(), event.getPosterUrl(),
                    summary == null ? null : summary.nextShowTime(),
                    summary == null ? null : summary.minPrice());
        }));
    }

    @Transactional(readOnly = true)
    public PublicEventResponse get(Long eventId) {
        // Drafts and cancelled events don't exist for the public
        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " not found"));
        List<ShowResponse> shows = showRepository
                .findByEventIdAndStartTimeAfterOrderByStartTimeAsc(eventId, Instant.now()).stream()
                .map(ShowResponse::from)
                .toList();
        return new PublicEventResponse(event.getId(), event.getTitle(), event.getDescription(),
                event.getCategory(), event.getPosterUrl(), shows);
    }
}