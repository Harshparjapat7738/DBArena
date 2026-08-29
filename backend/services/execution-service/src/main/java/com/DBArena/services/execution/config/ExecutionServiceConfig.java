package com.DBArena.services.execution.config;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.UlidIdGenerator;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Configuration
public class ExecutionServiceConfig {

    @Bean
    public IdGenerator idGenerator() {
        return new UlidIdGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ExecutionPolicy executionPolicy(ExecutionProperties properties) {
        ExecutionProperties.Policy p = properties.getPolicy();
        return new ExecutionPolicy(
                p.getMaxStatementLength(),
                p.getMaxResultRows(),
                p.getMaxResultBytes(),
                Duration.ofSeconds(p.getStatementTimeoutSeconds()),
                Duration.ofSeconds(p.getExplainTimeoutSeconds()),
                p.getMaxConcurrentPerUser(),
                p.getMaxConcurrentGlobal(),
                p.getSandboxConnectionLimit());
    }

    /** One virtual thread per in-flight execution - blocking JDBC by design (root CLAUDE.md); bounded not by this pool but by {@code globalConcurrencySemaphore} below. */
    @Bean
    public ExecutorService executionWorkerPool() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /** "Connection limit" (security requirement) at the service level - the actual cap on how many sandbox sessions can be live at once, platform-wide, regardless of how many requests arrive. */
    @Bean
    public Semaphore globalConcurrencySemaphore(ExecutionPolicy policy) {
        return new Semaphore(policy.maxConcurrentGlobal(), true);
    }
}
