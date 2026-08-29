package com.DBArena.common.testing.archunit.fixtures.dirty;

import org.springframework.util.StringUtils;

/** Fixture for {@code ArchitectureRulesTest}: deliberately depends on Spring so the rule has something to catch. */
public class UsesSpring {

    public boolean isBlank(String value) {
        return !StringUtils.hasText(value);
    }
}
