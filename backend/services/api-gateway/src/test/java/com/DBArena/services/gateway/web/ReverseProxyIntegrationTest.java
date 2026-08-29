package com.DBArena.services.gateway.web;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fake upstream is a plain JDK {@link HttpServer} (no extra dependency,
 * no real identity-service needed) so this proves the gateway's routing,
 * header/body passthrough, and auth-gating in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ReverseProxyIntegrationTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static HttpServer fakeUpstream;
    private static final AtomicReference<String> LAST_SEEN_PATH = new AtomicReference<>();
    private static final AtomicReference<String> LAST_SEEN_AUTH_HEADER = new AtomicReference<>();

    @AfterAll
    static void stopFakeUpstream() {
        if (fakeUpstream != null) {
            fakeUpstream.stop(0);
        }
    }

    /**
     * {@code @DynamicPropertySource} suppliers are evaluated lazily, exactly
     * when Spring resolves the property while preparing the ApplicationContext
     * - so starting the server from inside the supplier (idempotently)
     * guarantees it is listening before any property value referencing its
     * port is actually read, without depending on JUnit lifecycle ordering.
     */
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("dbarena.gateway.routes[0].prefix", () -> "/api/v1/auth");
        registry.add("dbarena.gateway.routes[0].uri", () -> "http://localhost:" + ensureFakeUpstreamRunning());
        registry.add("dbarena.security.jwt.secret", () -> SECRET);
    }

    private static synchronized int ensureFakeUpstreamRunning() {
        if (fakeUpstream == null) {
            try {
                fakeUpstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                fakeUpstream.createContext("/api/v1/auth", exchange -> {
                    LAST_SEEN_PATH.set(exchange.getRequestURI().toString());
                    LAST_SEEN_AUTH_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("X-Upstream-Marker", "identity-service");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                fakeUpstream.start();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start fake upstream", e);
            }
        }
        return fakeUpstream.getAddress().getPort();
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPathIsProxiedWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true}"))
                .andExpect(header().string("X-Upstream-Marker", "identity-service"));
    }

    @Test
    void protectedPathWithoutATokenIsRejectedBeforeReachingUpstream() throws Exception {
        LAST_SEEN_PATH.set(null);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.unauthenticated"));

        org.assertj.core.api.Assertions.assertThat(LAST_SEEN_PATH.get()).isNull();
    }

    @Test
    void protectedPathWithAValidTokenIsForwardedWithTheAuthorizationHeaderIntact() throws Exception {
        String token = signAccessToken("01J000USER");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(LAST_SEEN_AUTH_HEADER.get()).isEqualTo("Bearer " + token);
    }

    @Test
    void unroutedPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/unknown-service/ping"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("gateway.no_route"));
    }

    private static String signAccessToken(String userId) throws JOSEException {
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
}
