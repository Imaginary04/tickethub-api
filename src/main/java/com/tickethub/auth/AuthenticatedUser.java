package com.tickethub.auth;

import com.tickethub.user.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}