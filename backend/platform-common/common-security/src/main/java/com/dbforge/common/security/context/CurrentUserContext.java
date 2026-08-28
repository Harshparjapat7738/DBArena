package com.dbforge.common.security.context;

import com.dbforge.common.security.AuthenticatedUser;

import java.util.Optional;

/**
 * Request-scoped holder for the resolved {@link AuthenticatedUser}.
 * {@link com.dbforge.common.security.web.JwtAuthenticationFilter} sets
 * this at the start of a request and clears it in a {@code finally} block
 * so it can never leak across virtual threads pooled by the servlet
 * container (root CLAUDE.md requires virtual threads enabled - see
 * backend/CLAUDE.md "Rules specific to backend").
 */
public final class CurrentUserContext {

    private static final ThreadLocal<AuthenticatedUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(AuthenticatedUser user) {
        HOLDER.set(user);
    }

    public static Optional<AuthenticatedUser> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static AuthenticatedUser require() {
        return get().orElseThrow(() -> new IllegalStateException(
                "No AuthenticatedUser bound to this request - is JwtAuthenticationFilter registered?"));
    }

    public static void clear() {
        HOLDER.remove();
    }
}
