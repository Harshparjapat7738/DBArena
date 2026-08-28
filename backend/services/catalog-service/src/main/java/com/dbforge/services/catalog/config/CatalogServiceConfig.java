package com.dbforge.services.catalog.config;

import com.dbforge.common.core.id.IdGenerator;
import com.dbforge.common.core.id.UlidIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CatalogServiceConfig {

    @Bean
    public IdGenerator idGenerator() {
        return new UlidIdGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
