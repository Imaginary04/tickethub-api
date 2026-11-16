package com.tickethub.booking;

import com.tickethub.booking.dto.SeatMapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Public: GET /api/shows/** is open to everyone in SecurityConfig (seats are visible before logging in)
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;

    @GetMapping("/{showId}/seats")
    public SeatMapResponse getSeatMap(@PathVariable Long showId) {
        return seatMapService.getSeatMap(showId);
    }
}