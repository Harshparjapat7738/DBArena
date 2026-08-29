package com.dbforge.services.gateway.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteResolverTest {

    private final GatewayProperties properties = new GatewayProperties(List.of(
            new GatewayProperties.RouteRule("/api/v1/auth", "http://identity:8081"),
            new GatewayProperties.RouteRule("/api/v1/catalog", "http://catalog:8083"),
            new GatewayProperties.RouteRule("/api/v1/catalog/admin", "http://catalog-admin:8084")));

    private final GatewayRouteResolver resolver = new GatewayRouteResolver(properties);

    @Test
    void matchesTheConfiguredPrefix() {
        assertThat(resolver.resolve("/api/v1/auth/login")).contains(
                new GatewayProperties.RouteRule("/api/v1/auth", "http://identity:8081"));
    }

    @Test
    void picksTheLongestMatchingPrefix() {
        assertThat(resolver.resolve("/api/v1/catalog/admin/datasets")).contains(
                new GatewayProperties.RouteRule("/api/v1/catalog/admin", "http://catalog-admin:8084"));
        assertThat(resolver.resolve("/api/v1/catalog/problems")).contains(
                new GatewayProperties.RouteRule("/api/v1/catalog", "http://catalog:8083"));
    }

    @Test
    void returnsEmptyForAnUnroutedPath() {
        assertThat(resolver.resolve("/api/v1/unknown")).isEmpty();
    }
}
