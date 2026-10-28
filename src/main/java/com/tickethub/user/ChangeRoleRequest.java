package com.tickethub.user;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}