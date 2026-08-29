package com.DBArena.services.identity.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.RefreshTokenRecord;
import com.DBArena.services.identity.domain.UserAccount;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(RefreshTokenRecord token);

    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    void revoke(String tokenId, Instant revokedAt, Optional<String> replacedById);

    /** Used when reuse of an already-rotated token is detected - revokes every live token for the user. */
    void revokeAllForUser(TypedId<UserAccount> userId, Instant revokedAt);
}
