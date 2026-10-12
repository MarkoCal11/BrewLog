package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.model.RefreshToken;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.RefreshTokenRepository;
import hr.tvz.brewlog.brewlogapp.security.RefreshTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    public static final Duration REFRESH_TTL = Duration.ofHours(8);
    public static final Duration REMEMBER_ME_TTL = Duration.ofDays(30);

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /** What the controller needs after a rotation: the owner, new raw token, and its TTL for the cookie. */
    public record RefreshResult(User user, String rawToken, Duration ttl) {}

    public static Duration ttlFor(boolean rememberMe) {
        return rememberMe ? REMEMBER_ME_TTL : REFRESH_TTL;
    }

    /** Issue a new opaque refresh token (login/register). */
    public RefreshResult issue(User user, boolean rememberMe) {
        return persist(user, rememberMe);
    }

    /**
     * Rotate a refresh token: mark the presented one used and persist its successor.
     * Reuse of an already-used token is treated as theft — every refresh token the user
     * owns is dropped. noRollbackFor keeps that revocation committed despite the throw.
     */
    @Transactional(noRollbackFor = RefreshTokenException.class)
    public RefreshResult rotate(String presentedRawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(presentedRawToken))
                .orElseThrow(() -> new RefreshTokenException("Refresh token does not exist"));

        if (current.isUsed()) {
            refreshTokenRepository.deleteByUser(current.getUser());
            throw new RefreshTokenException("Refresh token reuse detected; all sessions revoked");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(current);
            throw new RefreshTokenException("Refresh token expired");
        }

        current.setUsed(true);
        refreshTokenRepository.save(current);

        return persist(current.getUser(), current.isRememberMe());
    }

    /** Invalidate a single refresh token (on logout). */
    public void revokeByRawToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Periodically drop refresh tokens past their expiry. Expired rows can never be validly
     * rotated (rotate() rejects them), so they're dead weight; used-but-unexpired rows are
     * kept so reuse detection still works during their TTL window. Runs hourly by default.
     */
    @Scheduled(cron = "${jwt.refresh-cleanup-cron:0 0 * * * *}")
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired refresh token(s)", deleted);
        }
    }

    private RefreshResult persist(User user, boolean rememberMe) {
        Duration ttl = ttlFor(rememberMe);
        String rawToken = generateRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plus(ttl))
                .used(false)
                .rememberMe(rememberMe)
                .createdAt(LocalDateTime.now())
                .build());
        return new RefreshResult(user, rawToken, ttl);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
