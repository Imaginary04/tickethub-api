package com.tickethub.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    // Event page: all shows with their venue and prices in ONE query.
    // Without @EntityGraph, reading show.getVenue() and show.getPrices() in a loop
    // would run 1 + 2N queries (the "N+1 problem").
    @EntityGraph(attributePaths = {"venue", "prices"})
    List<Show> findByEventIdOrderByStartTimeAsc(Long eventId);

    // A venue with shows can't be deleted
    boolean existsByVenueId(Long venueId);

    long countByEventId(Long eventId);

    // Publishing needs at least one show that hasn't started yet
    boolean existsByEventIdAndStartTimeAfter(Long eventId, Instant time);

    // Same rule as the EXCLUDE constraint in V4: two time ranges overlap when
    // each one starts before the other ends. Touching (end == start) is allowed.
    @Query("""
            select count(s) > 0 from Show s
            where s.venue.id = :venueId
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsOverlapping(@Param("venueId") Long venueId,
                              @Param("startTime") Instant startTime,
                              @Param("endTime") Instant endTime);
}