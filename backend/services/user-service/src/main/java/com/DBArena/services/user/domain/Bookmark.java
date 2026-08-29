package com.DBArena.services.user.domain;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;

/**
 * A learner's bookmark on a catalog problem. {@code userId} is
 * {@code TypedId<AuthenticatedUser>} - the same cross-service user-reference
 * type {@code common-security} already defines for the JWT subject - rather
 * than a locally-defined id type or, worse, a copy of identity-service's
 * email/displayName. This is what "no duplicated user identity data" means
 * in practice: this service knows a user only as an opaque id, and would
 * call identity-service (or accept a display name pushed at call time) if it
 * ever needed to render one.
 *
 * <p>{@code problemSlug} is a loose reference to catalog-service's Problem,
 * same convention as {@code Problem.datasetSlug()} - no cross-service
 * foreign key, hard rule #2.
 */
public record Bookmark(
        TypedId<Bookmark> id,
        TypedId<AuthenticatedUser> userId,
        String problemSlug,
        long bookmarkedAtEpochMillis) {

    public Bookmark {
        if (problemSlug == null || problemSlug.isBlank()) {
            throw new IllegalArgumentException("problemSlug must not be blank");
        }
    }
}
