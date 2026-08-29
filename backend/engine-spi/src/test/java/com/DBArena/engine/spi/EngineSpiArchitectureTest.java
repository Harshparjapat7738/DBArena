package com.DBArena.engine.spi;

import com.DBArena.common.testing.archunit.ArchitectureRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Live enforcement of root CLAUDE.md hard rule #1 for this module:
 * engine-spi must never depend on Spring. Only imports
 * {@code com.DBArena.engine.spi} main classes (not this test's own test
 * classpath, which is allowed to use JUnit/ArchUnit), so adding a
 * spring-boot-starter-* dependency to this module's compile scope is the
 * only way this test can start failing.
 */
class EngineSpiArchitectureTest {

    @Test
    void engineSpiHasNoSpringDependency() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.DBArena.engine.spi");

        ArchitectureRules.noSpringDependency("com.DBArena.engine.spi..").check(classes);
    }
}
