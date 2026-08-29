package com.dbforge.services.gateway.config;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

/** Longest-prefix-match route lookup. */
@Component
public class GatewayRouteResolver {

    private final GatewayProperties properties;

    public GatewayRouteResolver(GatewayProperties properties) {
        this.properties = properties;
    }

    public Optional<GatewayProperties.RouteRule> resolve(String path) {
        return properties.routes().stream()
                .filter(route -> path.startsWith(route.prefix()))
                .max(Comparator.comparingInt(route -> route.prefix().length()));
    }
}
