package com.dbforge.services.identity.security;

import com.dbforge.common.security.autoconfigure.CommonSecurityProperties;
import com.dbforge.common.security.jwt.JwtClaims;
import com.dbforge.services.identity.domain.UserAccount;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Mints access tokens (HS256, short TTL) with the same secret
 * common-security's {@code Hs256JwtVerifier} checks them against - see
 * that class's javadoc: "actual token issuance ... belongs to
 * identity-service". Never mints a refresh token as a JWT - refresh
 * tokens are opaque, see {@link RefreshTokenGenerator}, precisely so a
 * leaked access token can't be replayed as a refresh token or vice versa.
 */
@Component
public class JwtIssuer {

    private final CommonSecurityProperties securityProperties;
    private final Clock clock;

    public JwtIssuer(CommonSecurityProperties securityProperties, Clock clock) {
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    public String issueAccessToken(UserAccount user, Duration ttl) {
        try {
            Instant now = clock.instant();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.id().value())
                    .claim(JwtClaims.ROLES, List.copyOf(user.roles()))
                    .claim(JwtClaims.TOKEN_TYPE, JwtClaims.TOKEN_TYPE_ACCESS)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(ttl)))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretBytes()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }

    private byte[] secretBytes() {
        String secret = securityProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("dbforge.security.jwt.secret is not set");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
