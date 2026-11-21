package com.tickethub.booking;

import com.tickethub.auth.AuthenticatedUser;
import com.tickethub.booking.dto.HoldRequest;
import com.tickethub.booking.dto.HoldResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// Holding seats needs a login: only GET /api/shows/** is public in SecurityConfig
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class HoldController {

    private final BookingService bookingService;

    @PostMapping("/{showId}/holds")
    public ResponseEntity<HoldResponse> hold(@PathVariable Long showId,
                                             @Valid @RequestBody HoldRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser currentUser) {
        HoldResponse hold = bookingService.hold(showId, request, currentUser);
        return ResponseEntity.created(URI.create("/api/bookings/" + hold.bookingId())).body(hold);
    }
}