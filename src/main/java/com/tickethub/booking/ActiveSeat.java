package com.tickethub.booking;

// A seat that is currently taken in a show: PENDING = held, CONFIRMED = booked (filled by a JPQL query)
public record ActiveSeat(Long seatId, BookingStatus status) {
}