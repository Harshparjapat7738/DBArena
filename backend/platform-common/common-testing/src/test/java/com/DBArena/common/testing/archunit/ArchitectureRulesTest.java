package com.DBArena.common.testing.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves {@link ArchitectureRules#noSpringDependency} actually catches a
 * Spring dependency, against small fixture packages, so the rule can be
 * trusted before engine-spi (which has none of its own Spring-using code
 * to accidentally miss the check) applies it for real.
 */
class ArchitectureRulesTest {

    @Test
    void flagsAClassThatDependsOnSpring() {
        JavaClasses dirty = new ClassFileImporter()
                .importPackages("com.DBArena.common.testing.archunit.fixtures.dirty");

        EvaluationResult result = ArchitectureRules
                .noSpringDependency("com.DBArena.common.testing.archunit.fixtures.dirty..")
                .evaluate(dirty);

        assertThat(result.hasViolation()).isTrue();
    }

    @Test
    void allowsAFrameworkFreeClass() {
        JavaClasses clean = new ClassFileImporter()
                .importPackages("com.DBArena.common.testing.archunit.fixtures.clean");

        EvaluationResult result = ArchitectureRules
                .noSpringDependency("com.DBArena.common.testing.archunit.fixtures.clean..")
                .evaluate(clean);

        assertThat(result.hasViolation()).isFalse();
    }
}
