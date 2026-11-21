package com.tickethub.booking.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

// { "seatIds": [31, 32] }
public record HoldRequest(
        @NotEmpty List<@NotNull Long> seatIds
) {

    @AssertTrue(message = "each seat can only be picked once")
    public boolean isSeatIdsUnique() {
        if (seatIds == null) {
            return true;
        }
        List<Long> ids = seatIds.stream().filter(Objects::nonNull).toList();
        return ids.size() == ids.stream().distinct().count();
    }
}