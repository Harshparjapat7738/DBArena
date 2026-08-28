package com.dbforge.services.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Route table, e.g.:
 * <pre>
 * dbforge:
 *   gateway:
 *     routes:
 *       - prefix: /api/v1/auth
 *         uri: http://localhost:8081
 * </pre>
 * The gateway forwards the request's full original path unchanged - it
 * does not strip {@code prefix} - so a backend service's own
 * {@code @RequestMapping} must match the same path clients use.
 */
@ConfigurationProperties(prefix = "dbforge.gateway")
public record GatewayProperties(List<RouteRule> routes) {

    public GatewayProperties {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public record RouteRule(String prefix, String uri) {
    }
}
