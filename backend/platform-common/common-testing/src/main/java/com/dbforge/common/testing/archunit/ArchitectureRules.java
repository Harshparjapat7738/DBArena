package com.dbforge.common.testing.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reusable ArchUnit rule definitions for root CLAUDE.md's hard rules #1
 * and #2. Each module that the rule protects applies it against its own
 * {@code @AnalyzeClasses} scan - see engine-spi's
 * {@code EngineSpiArchitectureTest} for the concrete usage of
 * {@link #noSpringDependency}.
 *
 * <p>{@link #noServiceDependsOnAnotherService()} needs every service on
 * one ArchUnit import at once to be meaningful, so it is applied by a
 * dedicated architecture-test module once two or more
 * {@code services/*} modules exist (B09 onward) - a single-service
 * reactor has nothing for it to check yet.
 */
public final class ArchitectureRules {

    private ArchitectureRules() {
    }

    /**
     * Hard rule #1: {@code engine-spi} and {@code engine-adapters/*} must
     * not depend on Spring, so they stay usable outside a Spring
     * container (the sandbox agent, {@code dataset-cli}) and so the
     * driver layer can't accidentally couple to web/DI concerns.
     */
    public static ArchRule noSpringDependency(String... packageIdentifiers) {
        return noClasses()
                .that().resideInAnyPackage(packageIdentifiers)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .because("engine-spi and engine-adapters/* must stay framework-free "
                        + "(root CLAUDE.md hard rule #1) - enforced, not just documented");
    }

    /**
     * Hard rule #2: no {@code services/<x>} module may import
     * {@code services/<y>}; cross-service reads go through OpenFeign
     * clients, writes through Kafka events. Uses ArchUnit's slice-based
     * dependency check so any two distinct service packages under
     * {@code com.dbforge.services} are checked pairwise.
     */
    public static ArchRule noServiceDependsOnAnotherService() {
        return SlicesRuleDefinition.slices()
                .matching("com.dbforge.services.(*)..")
                .should().notDependOnEachOther()
                .because("cross-service reads go through OpenFeign, writes through Kafka events "
                        + "(root CLAUDE.md hard rule #2)");
    }

    /** Skip generated sources (Avro codegen, OpenAPI clients) when scanning - they're not hand-authored. */
    public static final ImportOption EXCLUDE_GENERATED =
            location -> !location.contains("/generated-sources/");
}
