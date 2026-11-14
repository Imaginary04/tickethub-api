package com.tickethub.event.dto;

import java.math.BigDecimal;
import java.time.Instant;

// Per event: the earliest matching show and the cheapest price (filled by a JPQL query)
public record ShowSummary(Long eventId, Instant nextShowTime, BigDecimal minPrice) {
}