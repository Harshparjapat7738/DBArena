package com.dbforge.common.security.jwt;

/** Claim names this platform agrees on across every service that mints or reads a token. */
public final class JwtClaims {

    private JwtClaims() {
    }

    /** Standard "sub" claim: the user id, as a bare TypedId value string. */
    public static final String SUBJECT = "sub";

    /** Custom claim: space-free list of role names, e.g. ["learner", "author"]. */
    public static final String ROLES = "roles";

    /** Custom claim: "access" or "refresh". A refresh token must never authenticate an API call. */
    public static final String TOKEN_TYPE = "typ";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
}
