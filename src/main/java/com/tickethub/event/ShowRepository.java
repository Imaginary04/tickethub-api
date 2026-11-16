package com.tickethub.event;
import com.tickethub.event.dto.ShowSummary;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {

    // Event page: all shows with their venue and prices in ONE query.
    // Without @EntityGraph, reading show.getVenue() and show.getPrices() in a loop
    // would run 1 + 2N queries (the "N+1 problem").
    @EntityGraph(attributePaths = {"venue", "prices"})
    List<Show> findByEventIdOrderByStartTimeAsc(Long eventId);

    // Seat map header: the show with its event, venue and prices in ONE query
    @EntityGraph(attributePaths = {"event", "venue", "prices"})
    Optional<Show> findWithDetailsById(Long id);

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

    // Public event page: only shows that haven't started, with venue and prices in one query
    @EntityGraph(attributePaths = {"venue", "prices"})
    List<Show> findByEventIdAndStartTimeAfterOrderByStartTimeAsc(Long eventId, Instant time);

    // Event cards: earliest show + cheapest price for a whole page of events in ONE query.
    // Same filters as the search; cityKey is "" for "any city" (no null parameters).
    @Query("""
            select new com.tickethub.event.dto.ShowSummary(s.event.id, min(s.startTime), min(p.price))
            from Show s
            join s.venue v
            join s.prices p
            where s.event.id in :eventIds
              and s.startTime >= :from
              and s.startTime < :to
              and (:cityKey = '' or lower(v.city) = :cityKey)
            group by s.event.id
            """)
    List<ShowSummary> summarize(@Param("eventIds") Collection<Long> eventIds,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                @Param("cityKey") String cityKey);
}