package com.DBArena.services.execution.config;

import com.DBArena.engine.adapters.postgres.PostgresConnectionFactory;
import com.DBArena.engine.adapters.postgres.PostgresEngineAdapter;
import com.DBArena.engine.spi.DatabaseEngineAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the already-built {@code adapter-postgres} module in with this
 * service's own sandbox credentials (never the platform's own Postgres
 * credentials, never anything client-supplied) - see
 * {@code ExecutionProperties.Postgres}'s Javadoc for what this role is
 * scoped to.
 */
@Configuration
@EnableConfigurationProperties(ExecutionProperties.class)
public class PostgresAdapterConfig {

    @Bean
    public PostgresConnectionFactory postgresConnectionFactory(ExecutionProperties properties) {
        ExecutionProperties.Postgres pg = properties.getPostgres();
        return new PostgresConnectionFactory(pg.getHost(), pg.getPort(), pg.getUsername(), pg.getPassword());
    }

    @Bean
    public DatabaseEngineAdapter postgresEngineAdapter(PostgresConnectionFactory connectionFactory) {
        return new PostgresEngineAdapter(connectionFactory);
    }
}
