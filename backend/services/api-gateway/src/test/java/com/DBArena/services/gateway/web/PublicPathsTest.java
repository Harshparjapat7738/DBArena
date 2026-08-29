package com.DBArena.services.gateway.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPathsTest {

    @Test
    void catalogBrowsingIsPublicOnlyForGet() {
        assertThat(PublicPaths.isPublic("GET", "/api/v1/catalog/problems")).isTrue();
        assertThat(PublicPaths.isPublic("GET", "/api/v1/catalog/problems/two-sum")).isTrue();
        assertThat(PublicPaths.isPublic("GET", "/api/v1/catalog/tags")).isTrue();

        assertThat(PublicPaths.isPublic("POST", "/api/v1/catalog/problems")).isFalse();
        assertThat(PublicPaths.isPublic("PUT", "/api/v1/catalog/problems/two-sum")).isFalse();
    }

    @Test
    void problemsAndDatasetsBrowsingIsPublicOnlyForGet() {
        assertThat(PublicPaths.isPublic("GET", "/api/v1/problems")).isTrue();
        assertThat(PublicPaths.isPublic("GET", "/api/v1/problems/two-sum/related")).isTrue();
        assertThat(PublicPaths.isPublic("GET", "/api/v1/datasets")).isTrue();
        assertThat(PublicPaths.isPublic("GET", "/api/v1/datasets/two-sum-dataset/schema")).isTrue();

        assertThat(PublicPaths.isPublic("POST", "/api/v1/problems")).isFalse();
        assertThat(PublicPaths.isPublic("DELETE", "/api/v1/datasets/two-sum-dataset")).isFalse();
    }

    @Test
    void methodAgnosticRoutesArePublicForAnyMethod() {
        assertThat(PublicPaths.isPublic("POST", "/api/v1/auth/login")).isTrue();
        assertThat(PublicPaths.isPublic("GET", "/actuator/health")).isTrue();
    }

    @Test
    void unlistedPathsAreNotPublic() {
        assertThat(PublicPaths.isPublic("GET", "/api/v1/auth/me")).isFalse();
        assertThat(PublicPaths.isPublic("GET", "/api/v1/unknown")).isFalse();
    }
}
