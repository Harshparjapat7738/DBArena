package com.DBArena.services.identity.config;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.UlidIdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityServiceConfig {

    @Bean
    public IdGenerator idGenerator() {
        return new UlidIdGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
