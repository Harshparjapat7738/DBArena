package com.DBArena.common.core.id;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class UlidIdGeneratorTest {

    @Test
    void generatesTwentySixCharacterCrockfordBase32Ids() {
        IdGenerator gen = new UlidIdGenerator();
        String id = gen.next();
        assertThat(id).hasSize(26);
        assertThat(id).matches("[0-9A-HJKMNP-TV-Z]{26}");
    }

    @Test
    void idsAreUniqueAcrossManyCalls() {
        IdGenerator gen = new UlidIdGenerator();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            assertThat(seen.add(gen.next())).isTrue();
        }
    }

    @Test
    void idsGeneratedInTheSameMillisecondAreStrictlyIncreasing() {
        Clock frozen = Clock.fixed(Instant.parse("2026-01-01T00:00:00.000Z"), ZoneOffset.UTC);
        IdGenerator gen = new UlidIdGenerator(frozen, new SecureRandom());

        String first = gen.next();
        String second = gen.next();
        String third = gen.next();

        assertThat(first.compareTo(second)).isLessThan(0);
        assertThat(second.compareTo(third)).isLessThan(0);
    }

    @Test
    void nextTypedWrapsInTypedId() {
        IdGenerator gen = new UlidIdGenerator();
        TypedId<Object> id = gen.nextTyped();
        assertThat(id.value()).hasSize(26);
    }
}
