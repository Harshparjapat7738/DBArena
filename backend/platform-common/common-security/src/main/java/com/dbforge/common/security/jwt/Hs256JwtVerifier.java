package com.dbforge.common.security.jwt;

import com.dbforge.common.core.id.TypedId;
import com.dbforge.common.security.AuthenticatedUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * HMAC-SHA256 (HS256) verifier - the default for local/dev environments
 * and for services that share a symmetric secret with identity-service.
 * A production deployment behind api-gateway may instead verify against
 * identity-service's published JWKS (RS256/ES256); that implementation
 * is added in B14 without changing this interface.
 */
public final class Hs256JwtVerifier implements JwtVerifier {

    private final MACVerifier macVerifier;
    private final Clock clock;

    public Hs256JwtVerifier(String secret, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("HS256 secret must be at least 32 bytes (256 bits)");
        }
        try {
            this.macVerifier = new MACVerifier(secret.getBytes(StandardCharsets.UTF_8));
        } catch (JOSEException e) {
            throw new IllegalArgumentException("Invalid HS256 secret", e);
        }
        this.clock = clock;
    }

    public Hs256JwtVerifier(String secret) {
        this(secret, Clock.systemUTC());
    }

    @Override
    public Optional<AuthenticatedUser> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(macVerifier)) {
                return Optional.empty();
            }

            var claims = jwt.getJWTClaimsSet();

            Instant expiry = claims.getExpirationTime() == null ? null : claims.getExpirationTime().toInstant();
            if (expiry == null || expiry.isBefore(clock.instant())) {
                return Optional.empty();
            }

            String tokenType = claims.getStringClaim(JwtClaims.TOKEN_TYPE);
            if (!JwtClaims.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                return Optional.empty();
            }

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }

            List<String> roles = claims.getStringListClaim(JwtClaims.ROLES);
            Set<String> roleSet = roles == null ? Set.of() : Set.copyOf(roles);

            return Optional.of(new AuthenticatedUser(TypedId.of(subject), roleSet, tokenType));
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }
}
