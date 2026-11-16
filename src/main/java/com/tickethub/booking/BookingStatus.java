package com.tickethub.booking;

public enum BookingStatus {
    PENDING,    // seats held, waiting for payment
    CONFIRMED,  // paid
    CANCELLED,  // given up or refunded
    EXPIRED     // hold ran out before payment
}