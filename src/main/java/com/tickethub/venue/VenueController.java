package com.tickethub.venue;

import com.tickethub.auth.AuthenticatedUser;
import com.tickethub.venue.dto.CreateVenueRequest;
import com.tickethub.venue.dto.VenueDetailsResponse;
import com.tickethub.venue.dto.VenueResponse;
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
@RequestMapping("/api/organizer/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        VenueResponse venue = venueService.create(request, currentUser);
        return ResponseEntity.created(URI.create("/api/organizer/venues/" + venue.id())).body(venue);
    }

    @GetMapping
    public List<VenueResponse> listMine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return venueService.listMine(currentUser);
    }

    @GetMapping("/{id}")
    public VenueDetailsResponse get(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return venueService.get(id, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        venueService.delete(id, currentUser);
    }
}