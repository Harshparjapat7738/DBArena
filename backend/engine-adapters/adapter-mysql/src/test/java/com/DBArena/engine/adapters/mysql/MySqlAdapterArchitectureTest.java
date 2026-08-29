package com.DBArena.engine.adapters.mysql;

import com.DBArena.common.testing.archunit.ArchitectureRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Live enforcement of root CLAUDE.md hard rule #1 for this module, same
 * pattern as adapter-postgres's {@code PostgresAdapterArchitectureTest}:
 * adapter-mysql must never depend on Spring, so it stays usable outside a
 * Spring container (a future sandbox agent, dataset-cli-style tooling).
 */
class MySqlAdapterArchitectureTest {

    @Test
    void adapterMysqlHasNoSpringDependency() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.DBArena.engine.adapters.mysql");

        ArchitectureRules.noSpringDependency("com.DBArena.engine.adapters.mysql..").check(classes);
    }
}
