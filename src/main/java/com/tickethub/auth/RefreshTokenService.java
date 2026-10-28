package com.tickethub.auth;

import com.tickethub.common.exception.UnauthorizedException;
import com.tickethub.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public record RotatedToken(User user, String refreshToken) {
    }

    /** Creates a new refresh token and returns the raw value. Only its hash is stored. */
    @Transactional
    public String create(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    /**
     * Swaps a valid refresh token for a new one. Reusing an old token revokes all of the user's tokens.
     * noRollbackFor keeps that revocation saved even though we throw afterwards.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RotatedToken rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = current.getUser();

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}. Revoking all sessions.", user.getId());
            refreshTokenRepository.revokeAllForUser(user.getId());
            throw new UnauthorizedException("Refresh token has been revoked. Please log in again.");
        }

        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        current.setRevoked(true);
        return new RotatedToken(user, create(user));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    /** Every night at 03:00. Revoked tokens are kept until they expire so reuse can still be detected. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        log.info("Deleted {} expired refresh tokens", deleted);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}