package com.DBArena.services.ai.web;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fake catalog-service and fake Groq/Gemini endpoints are all plain JDK
 * {@link HttpServer}s - same pattern api-gateway's ReverseProxyIntegrationTest
 * (M14) and catalog-service's ProblemControllerIntegrationTest (M13) already
 * established, extended here to a third external dependency shape (an LLM
 * HTTP API rather than another DBArena service). No Testcontainers - this
 * service has no database of its own.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class HintControllerIntegrationTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String GEMINI_MODEL = "test-gemini-model";

    private static HttpServer catalogServer;
    private static HttpServer groqServer;
    private static HttpServer geminiServer;

    private static final AtomicInteger GROQ_STATUS = new AtomicInteger(200);
    private static final AtomicInteger GEMINI_STATUS = new AtomicInteger(200);

    @TempDir
    static Path datasetsRoot;

    @BeforeEach
    void resetProviderStatuses() throws IOException {
        GROQ_STATUS.set(200);
        GEMINI_STATUS.set(200);
        Path entityDir = datasetsRoot.resolve("two-sum");
        Files.createDirectories(entityDir);
        Files.writeString(entityDir.resolve("dataset.yaml"), """
                datasetId: two-sum
                name: Two Sum
                schemaVersion: 1
                entities:
                  - name: numbers
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                    seedRows:
                      - id: 1
                """);
    }

    @AfterAll
    static void stopFakeServers() {
        if (catalogServer != null) catalogServer.stop(0);
        if (groqServer != null) groqServer.stop(0);
        if (geminiServer != null) geminiServer.stop(0);
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("dbarena.security.jwt.secret", () -> SECRET);
        registry.add("dbarena.ai.datasets-root", () -> datasetsRoot.toString());
        registry.add("dbarena.ai.catalog-service-uri", () -> "http://localhost:" + ensureCatalogServerRunning());
        registry.add("dbarena.ai.groq.api-key", () -> "test-groq-key");
        registry.add("dbarena.ai.groq.base-url", () -> "http://localhost:" + ensureGroqServerRunning() + "/chat");
        registry.add("dbarena.ai.gemini.api-key", () -> "test-gemini-key");
        registry.add("dbarena.ai.gemini.model", () -> GEMINI_MODEL);
        registry.add("dbarena.ai.gemini.base-url", () -> "http://localhost:" + ensureGeminiServerRunning());
    }

    private static synchronized int ensureCatalogServerRunning() {
        if (catalogServer == null) {
            try {
                catalogServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                catalogServer.createContext("/api/v1/catalog/problems/two-sum", exchange -> {
                    byte[] body = """
                            {"slug":"two-sum","title":"Two Sum","statementMarkdown":"Find two numbers that sum to target.",
                             "difficulty":"EASY","tags":["arrays"],"allowedEngines":["POSTGRES"],
                             "datasetSlug":"two-sum","published":true}
                            """.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                catalogServer.createContext("/api/v1/catalog/problems/missing-problem", exchange -> {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                });
                catalogServer.start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return catalogServer.getAddress().getPort();
    }

    private static synchronized int ensureGroqServerRunning() {
        if (groqServer == null) {
            try {
                groqServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                groqServer.createContext("/chat", exchange -> {
                    int status = GROQ_STATUS.get();
                    if (status != 200) {
                        exchange.sendResponseHeaders(status, -1);
                        exchange.close();
                        return;
                    }
                    byte[] body = "{\"choices\":[{\"message\":{\"content\":\"Think about a hash map.\"}}]}"
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                groqServer.start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return groqServer.getAddress().getPort();
    }

    private static synchronized int ensureGeminiServerRunning() {
        if (geminiServer == null) {
            try {
                geminiServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                geminiServer.createContext("/" + GEMINI_MODEL + ":generateContent", exchange -> {
                    int status = GEMINI_STATUS.get();
                    if (status != 200) {
                        exchange.sendResponseHeaders(status, -1);
                        exchange.close();
                        return;
                    }
                    byte[] body = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                            + "\"Consider a two-pointer approach instead.\"}]}}]}")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                geminiServer.start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return geminiServer.getAddress().getPort();
    }

    @Autowired
    private MockMvc mockMvc;

    private static String token(String userId) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("roles", List.of("learner"))
                .claim("typ", "access")
                .expirationTime(Date.from(now.plusSeconds(900)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static final String VALID_BODY = """
            {"learnerQuery":"SELECT * FROM numbers","errorOrResultText":"returns 0 rows","level":"APPROACH"}
            """;

    @Test
    void aValidRequestReturnsAGroqHintWhenGroqIsUp() throws Exception {
        mockMvc.perform(post("/api/v1/ai/problems/two-sum/hint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("01J000LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("groq"))
                .andExpect(jsonPath("$.hint").value("Think about a hash map."))
                .andExpect(jsonPath("$.level").value("APPROACH"))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    void fallsBackToGeminiWhenGroqIsDown() throws Exception {
        GROQ_STATUS.set(500);

        mockMvc.perform(post("/api/v1/ai/problems/two-sum/hint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("01J000LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("gemini"))
                .andExpect(jsonPath("$.hint").value("Consider a two-pointer approach instead."));
    }

    @Test
    void returns502WhenBothProvidersAreDown() throws Exception {
        GROQ_STATUS.set(500);
        GEMINI_STATUS.set(503);

        mockMvc.perform(post("/api/v1/ai/problems/two-sum/hint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("01J000LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ai.unavailable"));
    }

    @Test
    void aRequestWithNoTokenIs401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/problems/two-sum/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.unauthenticated"));
    }

    @Test
    void anUnknownProblemSlugIs404() throws Exception {
        mockMvc.perform(post("/api/v1/ai/problems/missing-problem/hint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("01J000LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource.not_found"));
    }

    @Test
    void aBlankLearnerQueryIs422() throws Exception {
        mockMvc.perform(post("/api/v1/ai/problems/two-sum/hint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("01J000LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learnerQuery":"","level":"CONCEPT"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("request.invalid"));
    }
}
