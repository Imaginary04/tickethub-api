package com.tickethub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // For the cleanup job: holds whose deadline has passed.
    // PENDING is written in the query (not a parameter), so Postgres can use the partial index from V5.
    @Query("""
            select b from Booking b
            where b.status = com.tickethub.booking.BookingStatus.PENDING
              and b.holdExpiresAt < :now
            """)
    List<Booking> findExpiredHolds(@Param("now") Instant now);

    // One hold per customer per show: stops one person from grabbing seats in many tabs
    boolean existsByUserIdAndShowIdAndStatus(Long userId, Long showId, BookingStatus status);
}