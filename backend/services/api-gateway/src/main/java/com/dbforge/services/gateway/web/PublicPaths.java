package com.dbforge.services.gateway.web;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/** Paths reachable without a valid access token. Everything else needs one, checked by {@link GatewayAccessFilter}. */
final class PublicPaths {

    private PublicPaths() {
    }

    private static final List<String> PATTERNS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html");

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    static boolean isPublic(String path) {
        return PATTERNS.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
    }
}
