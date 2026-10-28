package com.tickethub.auth;

import com.tickethub.auth.dto.AuthResponse;

/** What AuthService hands back to the controller: the JSON body plus the refresh token for the cookie. */
public record AuthResult(AuthResponse response, String refreshToken) {
}