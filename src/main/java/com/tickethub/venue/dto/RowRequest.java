package com.tickethub.venue.dto;

import com.tickethub.venue.SeatCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// One row of the layout, e.g. { "label": "A", "seatCount": 20, "category": "PLATINUM" }
public record RowRequest(
        @NotBlank @Pattern(regexp = "[A-Z]", message = "must be a single capital letter A-Z") String label,
        @Min(1) @Max(100) int seatCount,
        @NotNull SeatCategory category
) {
}