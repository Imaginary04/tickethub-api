package com.tickethub.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    // Event page: all shows with their venue and prices in ONE query.
    // Without @EntityGraph, reading show.getVenue() and show.getPrices() in a loop
    // would run 1 + 2N queries (the "N+1 problem").
    @EntityGraph(attributePaths = {"venue", "prices"})
    List<Show> findByEventIdOrderByStartTimeAsc(Long eventId);

    // A venue with shows can't be deleted
    boolean existsByVenueId(Long venueId);
}