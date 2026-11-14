package com.tickethub.event.dto;

import com.tickethub.event.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

// Used for both create (POST) and update (PUT)
public record EventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotNull EventCategory category,
        @URL @Size(max = 500) String posterUrl
) {
}