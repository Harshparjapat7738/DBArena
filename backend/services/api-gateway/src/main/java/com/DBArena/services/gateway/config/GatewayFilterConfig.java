package com.dbforge.services.gateway.config;

import com.dbforge.services.gateway.web.GatewayAccessFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class GatewayFilterConfig {

    @Bean
    public GatewayAccessFilter gatewayAccessFilter(ObjectMapper objectMapper) {
        return new GatewayAccessFilter(objectMapper);
    }

    @Bean
    public FilterRegistrationBean<Filter> gatewayAccessFilterRegistration(GatewayAccessFilter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        // After common-observability's CorrelationIdFilter (HIGHEST_PRECEDENCE) and
        // common-security's JwtAuthenticationFilter (HIGHEST_PRECEDENCE + 10) - this
        // filter needs CurrentUserContext already resolved.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
