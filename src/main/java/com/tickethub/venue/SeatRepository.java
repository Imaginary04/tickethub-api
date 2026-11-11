package com.tickethub.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Seat layout in display order: A1, A2, ... B1, B2, ...
    List<Seat> findByVenueIdOrderByRowLabelAscSeatNumberAsc(Long venueId);

    long countByVenueId(Long venueId);
}