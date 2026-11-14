package com.tickethub.event;

import com.tickethub.auth.AuthenticatedUser;
import com.tickethub.event.dto.CreateShowRequest;
import com.tickethub.event.dto.EventDetailsResponse;
import com.tickethub.event.dto.EventRequest;
import com.tickethub.event.dto.EventResponse;
import com.tickethub.event.dto.ShowResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// /api/organizer/** is already limited to ORGANIZER and ADMIN in SecurityConfig
@RestController
@RequestMapping("/api/organizer/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        EventResponse event = eventService.create(request, currentUser);
        return ResponseEntity.created(URI.create("/api/organizer/events/" + event.id())).body(event);
    }

    @GetMapping
    public List<EventResponse> listMine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return eventService.listMine(currentUser);
    }

    @GetMapping("/{id}")
    public EventDetailsResponse get(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return eventService.get(id, currentUser);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id,
                                @Valid @RequestBody EventRequest request,
                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return eventService.update(id, request, currentUser);
    }

    // Status changes are actions, not field edits, so they get their own endpoints
    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id,
                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return eventService.publish(id, currentUser);
    }

    @PostMapping("/{id}/cancel")
    public EventResponse cancel(@PathVariable Long id,
                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return eventService.cancel(id, currentUser);
    }

    @PostMapping("/{id}/shows")
    public ResponseEntity<ShowResponse> addShow(@PathVariable Long id,
                                                @Valid @RequestBody CreateShowRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ShowResponse show = eventService.addShow(id, request, currentUser);
        return ResponseEntity.created(URI.create("/api/organizer/events/" + id + "/shows/" + show.id())).body(show);
    }

    @DeleteMapping("/{id}/shows/{showId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShow(@PathVariable Long id,
                           @PathVariable Long showId,
                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        eventService.deleteShow(id, showId, currentUser);
    }
}