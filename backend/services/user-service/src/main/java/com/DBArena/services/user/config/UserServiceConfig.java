package com.DBArena.services.user.config;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.UlidIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UserServiceConfig {

    @Bean
    public IdGenerator idGenerator() {
        return new UlidIdGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
