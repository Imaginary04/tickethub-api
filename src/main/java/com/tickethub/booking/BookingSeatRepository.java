package com.tickethub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    // Seat map: which seats of this show are taken right now, and how (held or booked).
    // Uses the partial unique index (show_id, seat_id) WHERE active.
    @Query("""
            select new com.tickethub.booking.ActiveSeat(bs.seat.id, bs.booking.status)
            from BookingSeat bs
            where bs.show.id = :showId and bs.active = true
            """)
    List<ActiveSeat> findActiveSeatsByShowId(@Param("showId") Long showId);

    // Frees a booking's seats when it expires or is cancelled; the rows stay as history
    @Modifying
    @Query("update BookingSeat bs set bs.active = false where bs.booking.id = :bookingId and bs.active = true")
    int deactivateByBookingId(@Param("bookingId") Long bookingId);
}