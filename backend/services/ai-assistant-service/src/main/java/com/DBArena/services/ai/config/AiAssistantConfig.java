package com.DBArena.services.ai.config;

import com.DBArena.services.ai.dataset.DatasetContextLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiAssistantConfig {

    /** One shared client for both provider adapters - HttpClient is thread-safe and connection-pooling by design. */
    @Bean
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public DatasetContextLoader datasetContextLoader(AiProviderProperties properties) {
        return new DatasetContextLoader(Path.of(properties.getDatasetsRoot()));
    }

    /** Used by {@code HintRateLimiter} to window per-user hint requests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
