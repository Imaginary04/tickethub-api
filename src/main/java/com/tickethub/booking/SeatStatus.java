package com.tickethub.booking;

// What the customer sees for each seat on the map
public enum SeatStatus {
    AVAILABLE,
    HELD,    // someone has it in checkout (PENDING booking)
    BOOKED   // paid (CONFIRMED booking)
}