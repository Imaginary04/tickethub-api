package com.tickethub.venue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

public record CreateVenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 255) String address,
        @NotEmpty @Size(max = 26) List<@Valid RowRequest> rows
) {

    // Checked together with the annotations above; shows up as fieldErrors.rowLabelsUnique
    @AssertTrue(message = "row labels must be unique")
    public boolean isRowLabelsUnique() {
        if (rows == null) {
            return true;
        }
        List<String> labels = rows.stream()
                .filter(Objects::nonNull)
                .map(RowRequest::label)
                .filter(Objects::nonNull)
                .toList();
        return labels.size() == labels.stream().distinct().count();
    }
}