package com.DBArena.common.testing.archunit.fixtures.clean;

/** Fixture for {@code ArchitectureRulesTest}: a framework-free class the rule should let through. */
public class PlainJava {

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
