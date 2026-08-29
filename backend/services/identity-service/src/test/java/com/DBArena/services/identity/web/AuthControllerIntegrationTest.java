package com.DBArena.services.identity.web;

import com.DBArena.common.testing.containers.DBArenaMongoContainer;
import com.DBArena.services.identity.web.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack test: real Mongo (Testcontainers), real Mongock migration
 * run, real common-security JwtAuthenticationFilter and @CurrentUser
 * resolution - not a slice test, because the whole point of this
 * milestone is proving those pieces work together end to end. Was
 * Postgres/Flyway (M14) until the Mongo store swap - see MongoConfig's
 * Javadoc and backend/CLAUDE.md's Session Log; catalog-service's own
 * ProblemControllerIntegrationTest uses the identical Testcontainers-Mongo
 * + Mongock pattern this was rewritten to match.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

    // Computed once and reused by both suppliers below - the app's own
    // MongoConfig and Mongock's migration target must point at the exact
    // same database, or indexes get created in a database the app never
    // reads or writes (same pitfall catalog-service's own test flags).
    private static final String DATABASE_NAME = "identity_it_" + System.nanoTime();

    @Container
    static final DBArenaMongoContainer MONGO = DBArenaMongoContainer.defaultInstance();

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("dbarena.identity.mongo-uri", MONGO::getReplicaSetUrl);
        registry.add("dbarena.identity.mongo-database", () -> DATABASE_NAME);
        registry.add("mongock.mongo-db.uri", MONGO::getReplicaSetUrl);
        registry.add("mongock.mongo-db.database", () -> DATABASE_NAME);
        registry.add("dbarena.security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullSessionLifecycle() throws Exception {
        // register
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"correct-horse-battery","displayName":"Ada"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("learner"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andReturn();

        AuthResponse registerBody = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(), AuthResponse.class);
        Cookie firstRefreshCookie = registerResult.getResponse().getCookie(RefreshCookieFactory.COOKIE_NAME);
        assertThat(firstRefreshCookie).isNotNull();

        // /me with the access token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + registerBody.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"));

        // /me without a token is unauthenticated
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());

        // login with the same credentials
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"correct-horse-battery"}
                                """))
                .andExpect(status().isOk());

        // refresh rotates the cookie
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse refreshedBody = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), AuthResponse.class);
        Cookie rotatedRefreshCookie = refreshResult.getResponse().getCookie(RefreshCookieFactory.COOKIE_NAME);
        assertThat(rotatedRefreshCookie).isNotNull();
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(firstRefreshCookie.getValue());
        assertThat(refreshedBody.accessToken()).isNotEqualTo(registerBody.accessToken());

        // reusing the now-rotated (revoked) cookie is rejected AND revokes the whole chain
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isUnauthorized());

        // ... which means even the token that replaced it no longer works
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefreshCookie))
                .andExpect(status().isUnauthorized());

        // logout on a fresh login clears the cookie
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"correct-horse-battery"}
                                """))
                .andReturn();
        Cookie freshCookie = loginResult.getResponse().getCookie(RefreshCookieFactory.COOKIE_NAME);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(freshCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(freshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationIsConflict() throws Exception {
        String body = """
                {"email":"dup@example.com","password":"correct-horse-battery","displayName":"Dup"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("auth.email_already_registered"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpw@example.com","password":"correct-horse-battery","displayName":"X"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpw@example.com","password":"totally-wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_credentials"));
    }

    @Test
    void registrationRejectsAShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"short@example.com","password":"short","displayName":"X"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("request.invalid"));
    }
}
