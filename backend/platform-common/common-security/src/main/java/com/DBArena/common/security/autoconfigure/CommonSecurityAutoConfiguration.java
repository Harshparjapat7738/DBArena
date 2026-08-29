package com.dbforge.common.security.autoconfigure;

import com.dbforge.common.security.jwt.Hs256JwtVerifier;
import com.dbforge.common.security.jwt.JwtVerifier;
import com.dbforge.common.security.rbac.RoleAuthorizationAspect;
import com.dbforge.common.security.web.CurrentUserArgumentResolver;
import com.dbforge.common.security.web.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Wires JWT resolution, {@code @CurrentUser}, and (if AspectJ is on the
 * classpath) {@code @RequiresRole} into any Spring Boot service that
 * depends on common-security - no per-service configuration needed
 * beyond setting {@code dbforge.security.jwt.secret}.
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication
@EnableConfigurationProperties(CommonSecurityProperties.class)
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtVerifier.class)
    public JwtVerifier jwtVerifier(CommonSecurityProperties properties) {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "dbforge.security.jwt.secret is not set. Every service needs this to verify tokens; "
                            + "see common-security/CLAUDE.md (once written) for where it comes from per environment.");
        }
        return new Hs256JwtVerifier(properties.getSecret());
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtVerifier jwtVerifier) {
        return new JwtAuthenticationFilter(jwtVerifier);
    }

    @Bean
    public FilterRegistrationBean<Filter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        // Runs after common-observability's CorrelationIdFilter (HIGHEST_PRECEDENCE) so
        // auth failures still log with a correlation id.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public WebMvcConfigurer currentUserWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserArgumentResolver());
            }
        };
    }

    @Bean
    @ConditionalOnClass(Aspect.class)
    @ConditionalOnMissingBean(RoleAuthorizationAspect.class)
    public RoleAuthorizationAspect roleAuthorizationAspect() {
        return new RoleAuthorizationAspect();
    }
}
