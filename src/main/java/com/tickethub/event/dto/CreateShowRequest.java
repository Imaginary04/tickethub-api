package com.tickethub.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CreateShowRequest(
        @NotNull Long venueId,
        @NotNull @Future Instant startTime,
        @NotNull Instant endTime,
        @NotEmpty @Size(max = 3) List<@Valid PriceRequest> prices
) {

    @AssertTrue(message = "must be after startTime")
    public boolean isEndTimeAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    @AssertTrue(message = "each seat category can only have one price")
    public boolean isPriceCategoriesUnique() {
        if (prices == null) {
            return true;
        }
        List<?> categories = prices.stream()
                .filter(Objects::nonNull)
                .map(PriceRequest::category)
                .filter(Objects::nonNull)
                .toList();
        return categories.size() == categories.stream().distinct().count();
    }
}