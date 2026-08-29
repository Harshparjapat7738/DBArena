package com.DBArena.services.identity.domain;

import com.DBArena.common.core.id.TypedId;

import java.time.Instant;
import java.util.Set;

/**
 * A registered user. {@code passwordHash} never leaves this class's
 * package in any response DTO - see {@code web/dto/UserProfileResponse}
 * for the shape actually returned to callers.
 */
public record UserAccount(
        TypedId<UserAccount> id,
        String email,
        String passwordHash,
        String displayName,
        Set<String> roles,
        Instant createdAt) {

    public UserAccount {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        roles = Set.copyOf(roles);
    }

    public static final String DEFAULT_ROLE = "learner";
}
