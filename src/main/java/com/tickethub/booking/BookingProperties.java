package com.tickethub.booking;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        Duration holdDuration,
        int maxSeatsPerHold
) {
}