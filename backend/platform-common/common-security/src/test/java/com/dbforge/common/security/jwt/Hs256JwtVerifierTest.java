package com.dbforge.common.security.jwt;

import com.dbforge.common.security.AuthenticatedUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class Hs256JwtVerifierTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef"; // 33 bytes, >= 256 bits
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void verifiesAWellFormedAccessToken() throws JOSEException {
        String token = sign(JwtClaims.TOKEN_TYPE_ACCESS, "01J000USER", List.of("learner"),
                Instant.parse("2026-01-01T01:00:00Z"));

        Optional<AuthenticatedUser> result = new Hs256JwtVerifier(SECRET, FIXED_CLOCK).verify(token);

        assertThat(result).isPresent();
        assertThat(result.get().userId().value()).isEqualTo("01J000USER");
        assertThat(result.get().roles()).containsExactly("learner");
    }

    @Test
    void rejectsExpiredToken() throws JOSEException {
        String token = sign(JwtClaims.TOKEN_TYPE_ACCESS, "01J000USER", List.of("learner"),
                Instant.parse("2025-12-31T23:59:59Z"));

        assertThat(new Hs256JwtVerifier(SECRET, FIXED_CLOCK).verify(token)).isEmpty();
    }

    @Test
    void rejectsRefreshTokenPresentedAsAccessToken() throws JOSEException {
        String token = sign(JwtClaims.TOKEN_TYPE_REFRESH, "01J000USER", List.of("learner"),
                Instant.parse("2026-01-01T01:00:00Z"));

        assertThat(new Hs256JwtVerifier(SECRET, FIXED_CLOCK).verify(token)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() throws JOSEException {
        String token = sign(JwtClaims.TOKEN_TYPE_ACCESS, "01J000USER", List.of("learner"),
                Instant.parse("2026-01-01T01:00:00Z"));

        String otherSecret = "fedcba9876543210fedcba9876543210";
        assertThat(new Hs256JwtVerifier(otherSecret, FIXED_CLOCK).verify(token)).isEmpty();
    }

    @Test
    void rejectsGarbageInput() {
        assertThat(new Hs256JwtVerifier(SECRET, FIXED_CLOCK).verify("not-a-jwt")).isEmpty();
        assertThat(new Hs256JwtVerifier(SECRET, FIXED_CLOCK).verify(null)).isEmpty();
    }

    @Test
    void constructorRejectsShortSecret() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Hs256JwtVerifier("too-short"));
    }

    private static String sign(String tokenType, String subject, List<String> roles, Instant expiry)
            throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim(JwtClaims.ROLES, roles)
                .claim(JwtClaims.TOKEN_TYPE, tokenType)
                .expirationTime(Date.from(expiry))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
