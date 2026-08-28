package com.dbforge.common.security;

import com.dbforge.common.core.id.TypedId;

import java.util.Set;

/**
 * The identity resolved from a validated access token for the lifetime of
 * one request. {@code AuthenticatedUser} is itself the phantom type for
 * its own id - {@code userId} carries no other meaning than "the subject
 * of this token".
 */
public record AuthenticatedUser(TypedId<AuthenticatedUser> userId, Set<String> roles, String tokenType) {

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
        if (tokenType == null || tokenType.isBlank()) {
            throw new IllegalArgumentException("tokenType must not be blank");
        }
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... candidates) {
        for (String candidate : candidates) {
            if (roles.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
