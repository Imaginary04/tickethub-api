package com.tickethub.auth;

import com.tickethub.auth.dto.AuthResponse;
import com.tickethub.auth.dto.LoginRequest;
import com.tickethub.auth.dto.RegisterRequest;
import com.tickethub.common.exception.ConflictException;
import com.tickethub.common.exception.UnauthorizedException;
import com.tickethub.user.User;
import com.tickethub.user.UserRepository;
import com.tickethub.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        // Throws BadCredentialsException if the email or password is wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        return new AuthResult(buildResponse(rotated.user()), rotated.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthResult issueTokens(User user) {
        return new AuthResult(buildResponse(user), refreshTokenService.create(user));
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                UserResponse.from(user));
    }
}