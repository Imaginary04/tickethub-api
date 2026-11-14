package com.tickethub.event;

import com.tickethub.auth.AuthenticatedUser;
import com.tickethub.common.exception.BadRequestException;
import com.tickethub.common.exception.ConflictException;
import com.tickethub.common.exception.ResourceNotFoundException;
import com.tickethub.event.dto.CreateShowRequest;
import com.tickethub.event.dto.EventDetailsResponse;
import com.tickethub.event.dto.EventRequest;
import com.tickethub.event.dto.EventResponse;
import com.tickethub.event.dto.PriceRequest;
import com.tickethub.event.dto.ShowResponse;
import com.tickethub.user.Role;
import com.tickethub.user.UserRepository;
import com.tickethub.venue.SeatCategory;
import com.tickethub.venue.SeatRepository;
import com.tickethub.venue.Venue;
import com.tickethub.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String VENUE_BUSY = "The venue already has a show at that time";

    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Transactional
    public EventResponse create(EventRequest request, AuthenticatedUser currentUser) {
        Event event = new Event();
        event.setOrganizer(userRepository.getReferenceById(currentUser.id()));
        applyChanges(event, request);
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listMine(AuthenticatedUser currentUser) {
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(currentUser.id()).stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDetailsResponse get(Long eventId, AuthenticatedUser currentUser) {
        Event event = findOwnedEvent(eventId, currentUser);
        List<ShowResponse> shows = showRepository.findByEventIdOrderByStartTimeAsc(eventId).stream()
                .map(ShowResponse::from)
                .toList();
        return new EventDetailsResponse(event.getId(), event.getTitle(), event.getDescription(),
                event.getCategory(), event.getPosterUrl(), event.getStatus(),
                event.getCreatedAt(), event.getUpdatedAt(), shows);
    }

    @Transactional
    public EventResponse update(Long eventId, EventRequest request, AuthenticatedUser currentUser) {
        Event event = findOwnedEvent(eventId, currentUser);
        requireNotCancelled(event);
        applyChanges(event, request);
        // Flush now so @UpdateTimestamp sets updatedAt before we build the response
        return EventResponse.from(eventRepository.saveAndFlush(event));
    }

    // DRAFT -> PUBLISHED, only with at least one upcoming show
    @Transactional
    public EventResponse publish(Long eventId, AuthenticatedUser currentUser) {
        Event event = findOwnedEvent(eventId, currentUser);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ConflictException("Only DRAFT events can be published (this one is " + event.getStatus() + ")");
        }
        if (!showRepository.existsByEventIdAndStartTimeAfter(eventId, Instant.now())) {
            throw new ConflictException("Add at least one upcoming show before publishing");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return EventResponse.from(eventRepository.saveAndFlush(event));
    }

    // DRAFT or PUBLISHED -> CANCELLED (final)
    @Transactional
    public EventResponse cancel(Long eventId, AuthenticatedUser currentUser) {
        Event event = findOwnedEvent(eventId, currentUser);
        requireNotCancelled(event);
        event.setStatus(EventStatus.CANCELLED);
        return EventResponse.from(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public ShowResponse addShow(Long eventId, CreateShowRequest request, AuthenticatedUser currentUser) {
        Event event = findOwnedEvent(eventId, currentUser);
        requireNotCancelled(event);

        // The venue must belong to the event's organizer (404 otherwise, same as for venues)
        Venue venue = venueRepository.findById(request.venueId())
                .filter(v -> v.getOrganizer().getId().equals(event.getOrganizer().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Venue " + request.venueId() + " not found"));

        requirePricesMatchVenue(venue, request.prices());

        // Friendly check first...
        if (showRepository.existsOverlapping(venue.getId(), request.startTime(), request.endTime())) {
            throw new ConflictException(VENUE_BUSY);
        }

        Show show = new Show();
        show.setEvent(event);
        show.setVenue(venue);
        show.setStartTime(request.startTime());
        show.setEndTime(request.endTime());
        // setScale(2): 999 -> 999.00, same as the database stores it (validation already allows max 2 decimals)
        request.prices().forEach(p -> show.addPrice(p.category(), p.price().setScale(2)));

        // ...and the EXCLUDE constraint catches the race where two requests pass the check at once
        try {
            showRepository.saveAndFlush(show);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(VENUE_BUSY);
        }
        return ShowResponse.from(show);
    }

    @Transactional
    public void deleteShow(Long eventId, Long showId, AuthenticatedUser currentUser) {
        Event event = findOwnedEvent(eventId, currentUser);
        Show show = showRepository.findById(showId)
                .filter(s -> s.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));
        if (event.getStatus() == EventStatus.PUBLISHED && showRepository.countByEventId(eventId) == 1) {
            throw new ConflictException("A published event must keep at least one show");
        }
        showRepository.delete(show); // prices go too (cascade)
    }

    // Not yours = 404, same rule as venues
    private Event findOwnedEvent(Long eventId, AuthenticatedUser currentUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " not found"));
        boolean isOwner = event.getOrganizer().getId().equals(currentUser.id());
        if (!isOwner && currentUser.role() != Role.ADMIN) {
            throw new ResourceNotFoundException("Event " + eventId + " not found");
        }
        return event;
    }

    private void requireNotCancelled(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("Event is cancelled and can't be changed");
        }
    }

    // Every seat category in the venue needs a price, and no price for categories it doesn't have
    private void requirePricesMatchVenue(Venue venue, List<PriceRequest> prices) {
        Set<SeatCategory> venueCategories = seatRepository.findCategoriesByVenueId(venue.getId());
        Set<SeatCategory> pricedCategories = prices.stream()
                .map(PriceRequest::category)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SeatCategory.class)));
        if (!pricedCategories.equals(venueCategories)) {
            throw new BadRequestException("Prices must cover exactly this venue's seat categories: "
                    + venueCategories.stream().sorted().toList());
        }
    }

    private void applyChanges(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setPosterUrl(request.posterUrl());
    }
}