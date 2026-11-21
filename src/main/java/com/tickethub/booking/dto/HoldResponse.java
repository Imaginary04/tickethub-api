package com.tickethub.booking.dto;

import com.tickethub.booking.BookingStatus;
import com.tickethub.venue.SeatCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// The new PENDING booking: what the checkout page shows (seats, total, countdown deadline)
public record HoldResponse(
        Long bookingId,
        Long showId,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant holdExpiresAt,
        List<HeldSeat> seats
) {

    public record HeldSeat(Long seatId, String row, int number, SeatCategory category, BigDecimal price) {
    }
}