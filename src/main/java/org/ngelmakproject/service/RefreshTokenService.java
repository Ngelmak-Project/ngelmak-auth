package org.ngelmakproject.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.ngelmakproject.domain.RefreshToken;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for the given user with the specified TTL.
     * <p>
     * The token is generated using a random UUID, marked as active (not revoked),
     * and assigned an expiry date based on the provided duration.
     *
     * @param user the user owning the token
     * @param ttl  duration until the token expires
     * @return the newly created refresh token
     */
    public RefreshToken create(User user, Duration ttl) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(Instant.now().plus(ttl));
        rt.setRevoked(false);
        return refreshTokenRepository.save(rt);
    }

    /**
     * Attempts to rotate a refresh token.
     * <p>
     * If the provided token exists, is not revoked, and is not expired, it will be
     * extended with a new expiry date based on the provided TTL and marked as
     * revoked. The updated token is then returned.
     * Otherwise, {@code Optional.empty()} is returned.
     *
     * @param token the refresh token string
     * @param ttl   duration until the new token expires
     * @return an Optional containing the new refresh token, or empty if rotation
     *         fails
     */
    public Optional<RefreshToken> rotate(String token, Duration ttl) {
        // Find existing valid token
        Optional<RefreshToken> optional = refreshTokenRepository.findByTokenAndRevokedFalseAndExpiresAtAfter(
                token,
                Instant.now());

        if (optional.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken existing = optional.get();

        // Extend the existing token's expiry date
        existing.setRevoked(false);
        existing.setToken(UUID.randomUUID().toString());
        existing.setExpiresAt(Instant.now().plus(ttl));

        // Persist and return
        return Optional.of(refreshTokenRepository.save(existing));
    }

    /**
     * Revokes all active (non‑revoked) refresh tokens for the given user.
     *
     * @param userId the user whose active tokens should be revoked
     */
    public void revokeAllForUser(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);

        tokens.forEach(rt -> rt.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }

    /**
     * Revokes a single active refresh token if it exists and is not already
     * revoked.
     *
     * @param token the refresh token string to revoke
     */
    public void revoke(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

}
