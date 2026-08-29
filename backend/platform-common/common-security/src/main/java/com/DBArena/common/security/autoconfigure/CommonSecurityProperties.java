package com.dbforge.common.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dbforge.security.jwt")
public class CommonSecurityProperties {

    /**
     * HS256 shared secret. Required in every environment except tests that
     * supply their own {@code JwtVerifier} bean. Must be >= 32 bytes - see
     * {@link com.dbforge.common.security.jwt.Hs256JwtVerifier}. Never commit
     * a real value; local dev reads this from an env var, production from
     * the secret store (see docs/01, once written).
     */
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
