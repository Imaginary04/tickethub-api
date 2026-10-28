package com.tickethub.auth;

import com.tickethub.auth.dto.AuthResponse;
import com.tickethub.auth.dto.LoginRequest;
import com.tickethub.auth.dto.RegisterRequest;
import com.tickethub.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory cookieFactory;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withRefreshCookie(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        return withRefreshCookie(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(result.response());
    }
}