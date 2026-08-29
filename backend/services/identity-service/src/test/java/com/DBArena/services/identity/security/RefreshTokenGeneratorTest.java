package com.DBArena.services.identity.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator generator = new RefreshTokenGenerator();

    @Test
    void generatesUniqueTokens() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(seen.add(generator.generate())).isTrue();
        }
    }

    @Test
    void hashIsDeterministicAndDoesNotRevealTheToken() {
        String token = generator.generate();
        String hash1 = generator.hash(token);
        String hash2 = generator.hash(token);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(token);
    }

    @Test
    void differentTokensHashDifferently() {
        String hashA = generator.hash(generator.generate());
        String hashB = generator.hash(generator.generate());
        assertThat(hashA).isNotEqualTo(hashB);
    }
}
