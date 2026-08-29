package com.dbforge.services.ai.provider;

import com.dbforge.services.ai.config.AiProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Groq's OpenAI-compatible {@code /chat/completions} endpoint, called with
 * the JDK's own {@link HttpClient} - no AI SDK dependency, matching the
 * "plain driver, not a framework" posture every other service in this
 * reactor already takes with its own external system (plain JDBC, plain
 * Mongo driver, hand-rolled reverse-proxy client). This is the primary
 * provider; {@link GeminiCompletionClient} is the fallback -
 * {@code FallbackAiCompletionGateway} decides between them, this class
 * doesn't know it has a fallback.
 */
@Component
public class GroqCompletionClient implements AiCompletionClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiProviderProperties.Groq config;

    public GroqCompletionClient(HttpClient httpClient, ObjectMapper objectMapper, AiProviderProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.config = properties.getGroq();
    }

    @Override
    public String name() {
        return "groq";
    }

    @Override
    public boolean configured() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public AiCompletionResult complete(AiCompletionRequest request) {
        if (!configured()) {
            throw new AiProviderException(name(), "no API key configured (dbforge.ai.groq.api-key)");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        body.put("max_tokens", request.maxOutputTokens());
        body.put("temperature", 0.3);
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", request.systemPrompt());
        messages.addObject().put("role", "user").put("content", request.userPrompt());

        HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        } catch (Exception e) {
            throw new AiProviderException(name(), "could not build request", e);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiProviderException(name(), "request failed: " + e.getClass().getSimpleName(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiProviderException(name(), "HTTP " + response.statusCode());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("choices").path(0).path("message").path("content").asText(null);
            if (text == null || text.isBlank()) {
                throw new AiProviderException(name(), "empty completion in response");
            }
            return new AiCompletionResult(text, name());
        } catch (IOException e) {
            throw new AiProviderException(name(), "could not parse response", e);
        }
    }
}
