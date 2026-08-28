package com.dbforge.services.identity.web;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * The only place that builds the refresh-token cookie. Root CLAUDE.md
 * hard rule #6: {@code HttpOnly; Secure; SameSite=Strict}, always -
 * every attribute below is load-bearing, not a default to tune away.
 */
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "dbforge_rt";
    private static final String COOKIE_PATH = "/api/v1/auth";

    public ResponseCookie issue(String refreshToken, Instant expiresAt) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.between(Instant.now(), expiresAt))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
