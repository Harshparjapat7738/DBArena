package com.dbforge.services.gateway.web;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Paths (optionally scoped to one HTTP method) reachable without a valid
 * access token. Everything else needs one, checked by
 * {@link GatewayAccessFilter}. A {@code null} method matches any method -
 * used for the handful of routes that are public regardless of verb
 * (auth's own register/login/refresh, actuator, swagger). Catalog browsing
 * (M13) is the first route that's public for {@code GET} only - its
 * writes (create/update/publish) still require a token, enforced again,
 * independently, by catalog-service's own {@code @RequiresRole("admin")}.
 */
final class PublicPaths {

    private PublicPaths() {
    }

    private record PublicRoute(String method, String pattern) {
    }

    private static final List<PublicRoute> ROUTES = List.of(
            new PublicRoute(null, "/api/v1/auth/register"),
            new PublicRoute(null, "/api/v1/auth/login"),
            new PublicRoute(null, "/api/v1/auth/refresh"),
            new PublicRoute(null, "/actuator/**"),
            new PublicRoute(null, "/v3/api-docs/**"),
            new PublicRoute(null, "/swagger-ui/**"),
            new PublicRoute(null, "/swagger-ui.html"),
            new PublicRoute("GET", "/api/v1/catalog/problems"),
            new PublicRoute("GET", "/api/v1/catalog/problems/**"),
            new PublicRoute("GET", "/api/v1/catalog/tags"));

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    static boolean isPublic(String method, String path) {
        return ROUTES.stream().anyMatch(route ->
                (route.method() == null || route.method().equalsIgnoreCase(method))
                        && MATCHER.match(route.pattern(), path));
    }
}
