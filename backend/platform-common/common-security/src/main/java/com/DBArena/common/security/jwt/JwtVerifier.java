package com.DBArena.common.security.jwt;

import com.DBArena.common.security.AuthenticatedUser;

import java.util.Optional;

/**
 * Verifies an access token and, if valid, resolves it to an
 * {@link AuthenticatedUser}. Returns {@link Optional#empty()} for any
 * invalid token (bad signature, expired, wrong token type) rather than
 * throwing - callers decide how to respond to "unauthenticated", this
 * type only answers "is this token good".
 */
public interface JwtVerifier {

    Optional<AuthenticatedUser> verify(String token);
}
