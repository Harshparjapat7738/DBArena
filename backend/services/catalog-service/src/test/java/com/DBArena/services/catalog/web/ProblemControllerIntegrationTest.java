package com.DBArena.services.catalog.web;

import com.DBArena.common.testing.containers.DBArenaMongoContainer;
import com.DBArena.services.catalog.domain.TagCount;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack test: real Mongo (Testcontainers), real Mongock migration run,
 * real common-security JwtAuthenticationFilter + @RequiresRole enforcement
 * - same posture as identity-service's AuthControllerIntegrationTest (M14):
 * not a slice test, because the point is proving the pieces work together.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ProblemControllerIntegrationTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    // Computed once and reused by both suppliers below - the app's own
    // MongoConfig and Mongock's migration target must point at the exact
    // same database, or indexes get created in a database the app never
    // reads or writes.
    private static final String DATABASE_NAME = "catalog_it_" + System.nanoTime();

    @Container
    static final DBArenaMongoContainer MONGO = DBArenaMongoContainer.defaultInstance();

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("dbarena.catalog.mongo-uri", MONGO::getReplicaSetUrl);
        registry.add("dbarena.catalog.mongo-database", () -> DATABASE_NAME);
        registry.add("mongock.mongo-db.uri", MONGO::getReplicaSetUrl);
        registry.add("mongock.mongo-db.database", () -> DATABASE_NAME);
        registry.add("dbarena.security.jwt.secret", () -> SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String token(String userId, String... roles) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("roles", List.of(roles))
                .claim("typ", "access")
                .expirationTime(Date.from(now.plusSeconds(900)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static String adminToken() throws JOSEException {
        return token("01J000ADMIN", "admin");
    }

    private static String learnerToken() throws JOSEException {
        return token("01J000LEARNER", "learner");
    }

    private static String createRequestBody(String slug) {
        return """
                {"slug":"%s","title":"Two Sum","statementMarkdown":"Given nums...","difficulty":"EASY",
                 "tags":["arrays","hash-map"],"allowedEngines":["POSTGRES","MONGODB"],"datasetSlug":"two-sum-dataset"}
                """.formatted(slug);
    }

    @Test
    void unpublishedProblemIs404UntilPublishedThenVisibleToAnonymousBrowsing() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("two-sum")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false));

        // not visible while unpublished - 404, not "exists but forbidden"
        mockMvc.perform(get("/api/v1/catalog/problems/two-sum"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource.not_found"));

        mockMvc.perform(post("/api/v1/catalog/problems/two-sum/publish")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));

        // now visible with no token at all
        mockMvc.perform(get("/api/v1/catalog/problems/two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.tags", org.hamcrest.Matchers.containsInAnyOrder("arrays", "hash-map")));
    }

    @Test
    void creatingAProblemWithoutAdminRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("forbidden-case")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));

        mockMvc.perform(post("/api/v1/catalog/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("forbidden-case-anon")))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateSlugIsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("dup-slug")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/catalog/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody("dup-slug")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("catalog.slug_already_exists"));
    }

    @Test
    void createRejectsAMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"no-title","statementMarkdown":"x","difficulty":"EASY",
                                 "allowedEngines":["POSTGRES"]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("request.invalid"));
    }

    @Test
    void listProblemsFiltersByDifficultyAndPaginates() throws Exception {
        for (int i = 0; i < 3; i++) {
            String slug = "list-" + i;
            mockMvc.perform(post("/api/v1/catalog/problems")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequestBody(slug)))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/v1/catalog/problems/" + slug + "/publish")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/catalog/problems").param("limit", "2").param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());

        MvcResult tagsResult = mockMvc.perform(get("/api/v1/catalog/tags"))
                .andExpect(status().isOk())
                .andReturn();
        List<TagCount> tagCounts = objectMapper.readValue(
                tagsResult.getResponse().getContentAsString(), new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TagCount arraysTag = tagCounts.stream().filter(tc -> tc.tag().equals("arrays")).findFirst().orElseThrow();
        assertThat(arraysTag.count()).isEqualTo(3L);
    }
}
