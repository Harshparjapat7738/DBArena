package com.DBArena.services.identity.security;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.autoconfigure.CommonSecurityProperties;
import com.DBArena.common.security.jwt.Hs256JwtVerifier;
import com.DBArena.services.identity.domain.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the token this class mints is actually accepted by
 * common-security's verifier - the two live in different modules and
 * nothing at compile time enforces they agree on claim names or the
 * signing algorithm.
 */
class JwtIssuerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedTokenVerifiesAndRoundTripsClaims() {
        CommonSecurityProperties properties = new CommonSecurityProperties();
        properties.setSecret(SECRET);
        JwtIssuer issuer = new JwtIssuer(properties, FIXED_CLOCK);

        UserAccount user = new UserAccount(
                TypedId.of("01J000USER"), "learner@example.com", "hash", "Ada",
                Set.of("learner", "author"), Instant.parse("2025-01-01T00:00:00Z"));

        String token = issuer.issueAccessToken(user, Duration.ofMinutes(15));

        Optional<AuthenticatedUser> verified = new Hs256JwtVerifier(SECRET, FIXED_CLOCK).verify(token);

        assertThat(verified).isPresent();
        assertThat(verified.get().userId().value()).isEqualTo("01J000USER");
        assertThat(verified.get().roles()).containsExactlyInAnyOrder("learner", "author");
    }

    @Test
    void tokenIsExpiredAfterItsTtl() {
        CommonSecurityProperties properties = new CommonSecurityProperties();
        properties.setSecret(SECRET);
        JwtIssuer issuer = new JwtIssuer(properties, FIXED_CLOCK);

        UserAccount user = new UserAccount(
                TypedId.of("01J000USER"), "learner@example.com", "hash", "Ada",
                Set.of("learner"), Instant.parse("2025-01-01T00:00:00Z"));

        String token = issuer.issueAccessToken(user, Duration.ofMinutes(15));

        Clock later = Clock.fixed(FIXED_CLOCK.instant().plus(Duration.ofMinutes(16)), ZoneOffset.UTC);
        assertThat(new Hs256JwtVerifier(SECRET, later).verify(token)).isEmpty();
    }
}
