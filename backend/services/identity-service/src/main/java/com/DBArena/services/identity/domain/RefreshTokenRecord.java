package com.DBArena.services.identity.domain;

import com.DBArena.common.core.id.TypedId;

import java.time.Instant;
import java.util.Optional;

/**
 * One row of the refresh_tokens table. {@code tokenHash} is
 * SHA-256(opaque token) - see {@link com.DBArena.services.identity.security.RefreshTokenGenerator}.
 * {@code replacedById} chains rotations so a reused (already-rotated)
 * token can be traced and the whole chain revoked - see
 * {@code AuthService#detectReuseAndRevokeChain}.
 */
public record RefreshTokenRecord(
        String id,
        TypedId<UserAccount> userId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Optional<Instant> revokedAt,
        Optional<String> replacedById) {

    public RefreshTokenRecord {
        revokedAt = revokedAt == null ? Optional.empty() : revokedAt;
        replacedById = replacedById == null ? Optional.empty() : replacedById;
    }

    public boolean isRevoked() {
        return revokedAt.isPresent();
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }
}
