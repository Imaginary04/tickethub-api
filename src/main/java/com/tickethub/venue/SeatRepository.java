package com.tickethub.venue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Seat layout in display order: A1, A2, ... B1, B2, ...
    List<Seat> findByVenueIdOrderByRowLabelAscSeatNumberAsc(Long venueId);

    long countByVenueId(Long venueId);

    // Which categories this venue actually has, e.g. [GOLD, SILVER]
    @Query("select distinct s.category from Seat s where s.venue.id = :venueId")
    Set<SeatCategory> findCategoriesByVenueId(@Param("venueId") Long venueId);
}