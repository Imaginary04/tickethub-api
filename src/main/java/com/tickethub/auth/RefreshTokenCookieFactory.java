package com.tickethub.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final JwtProperties jwtProperties;

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, jwtProperties.refreshTokenTtl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}