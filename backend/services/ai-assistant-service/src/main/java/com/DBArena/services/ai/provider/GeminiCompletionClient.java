package com.DBArena.services.ai.provider;

import com.DBArena.services.ai.config.AiProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Google's Gemini {@code generateContent} REST endpoint - the fallback
 * provider, used only when {@link GroqCompletionClient} fails or isn't
 * configured (see {@code FallbackAiCompletionGateway}). Same "plain
 * HttpClient, no SDK" posture as the Groq client.
 */
@Component
public class GeminiCompletionClient implements AiCompletionClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiProviderProperties.Gemini config;

    public GeminiCompletionClient(HttpClient httpClient, ObjectMapper objectMapper, AiProviderProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.config = properties.getGemini();
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean configured() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public AiCompletionResult complete(AiCompletionRequest request) {
        if (!configured()) {
            throw new AiProviderException(name(), "no API key configured (DBArena.ai.gemini.api-key)");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", request.systemPrompt());
        var contents = body.putArray("contents");
        var userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", request.userPrompt());
        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", request.maxOutputTokens());
        generationConfig.put("temperature", 0.3);

        String uri = config.getBaseUrl() + "/" + config.getModel() + ":generateContent?key="
                + java.net.URLEncoder.encode(config.getApiKey(), StandardCharsets.UTF_8);

        HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .header("Content-Type", "application/json")
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
            String text = root.path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new AiProviderException(name(), "empty completion in response");
            }
            return new AiCompletionResult(text, name());
        } catch (IOException e) {
            throw new AiProviderException(name(), "could not parse response", e);
        }
    }
}
