package com.dbforge.services.identity.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashIsNotThePlaintext() {
        String hash = hasher.hash("correct horse battery staple");
        assertThat(hash).doesNotContain("correct horse battery staple");
    }

    @Test
    void matchesTheOriginalPassword() {
        String hash = hasher.hash("correct horse battery staple");
        assertThat(hasher.matches("correct horse battery staple", hash)).isTrue();
    }

    @Test
    void rejectsAWrongPassword() {
        String hash = hasher.hash("correct horse battery staple");
        assertThat(hasher.matches("wrong password", hash)).isFalse();
    }

    @Test
    void twoHashesOfTheSamePasswordDiffer() {
        String a = hasher.hash("same password");
        String b = hasher.hash("same password");
        assertThat(a).isNotEqualTo(b); // salted
        assertThat(hasher.matches("same password", a)).isTrue();
        assertThat(hasher.matches("same password", b)).isTrue();
    }
}
