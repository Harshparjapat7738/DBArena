package com.dbforge.engine.spi.cdm;

import com.dbforge.common.core.error.FieldViolation;
import com.dbforge.common.core.value.CdmValue;
import com.dbforge.common.core.value.CdmValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Structural validation for an authored {@link CdmDataset} - everything a
 * constructor alone can't check because it needs to see more than one
 * field, or more than one entity, at once. Framework-free (engine-spi hard
 * rule #1) so B12 (problem-validator), B18 (ingestion-service), and
 * tools/dataset-cli (this milestone) can all call the same validator
 * instead of each reimplementing it.
 *
 * <p>Every check that fails adds a {@link FieldViolation} and continues -
 * this never throws for a malformed dataset, it reports everything wrong
 * with it in one pass. A dataset an author is actively authoring is
 * expected to be invalid sometimes; that's the whole point of a validator
 * they run repeatedly while writing YAML.
 */
public final class CdmDatasetValidator {

    private CdmDatasetValidator() {
    }

    public static CdmValidationResult validate(CdmDataset dataset) {
        List<FieldViolation> violations = new ArrayList<>();

        validateEntityNamesUnique(dataset, violations);

        Map<String, CdmEntity> entitiesByName = new LinkedHashMap<>();
        for (CdmEntity entity : dataset.entities()) {
            entitiesByName.putIfAbsent(entity.name(), entity);
        }

        for (int i = 0; i < dataset.entities().size(); i++) {
            CdmEntity entity = dataset.entities().get(i);
            String entityPath = "entities[" + i + "]";
            validateColumnNamesUnique(entity, entityPath, violations);
            validatePrimaryKeyPresent(entity, entityPath, violations);
            validateForeignKeys(entity, entityPath, entitiesByName, violations);
            validateSeedRows(entity, entityPath, entitiesByName, violations);
        }

        return new CdmValidationResult(violations);
    }

    private static void validateEntityNamesUnique(CdmDataset dataset, List<FieldViolation> violations) {
        Set<String> seen = new HashSet<>();
        List<CdmEntity> entities = dataset.entities();
        for (int i = 0; i < entities.size(); i++) {
            String name = entities.get(i).name();
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                violations.add(new FieldViolation("entities[" + i + "].name",
                        "duplicate entity name (case-insensitive, to avoid a Postgres identifier-folding "
                                + "collision at materialization time): '" + name + "'"));
            }
        }
    }

    private static void validateColumnNamesUnique(CdmEntity entity, String entityPath, List<FieldViolation> violations) {
        Set<String> seen = new HashSet<>();
        List<CdmColumn> columns = entity.columns();
        for (int i = 0; i < columns.size(); i++) {
            String name = columns.get(i).name();
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                violations.add(new FieldViolation(entityPath + ".columns[" + i + "].name",
                        "duplicate column name (case-insensitive) in entity '" + entity.name() + "': '" + name + "'"));
            }
        }
    }

    private static void validatePrimaryKeyPresent(CdmEntity entity, String entityPath, List<FieldViolation> violations) {
        if (entity.primaryKeyColumns().isEmpty()) {
            violations.add(new FieldViolation(entityPath + ".columns",
                    "entity '" + entity.name() + "' must declare at least one primary-key column"));
        }
    }

    private static void validateForeignKeys(
            CdmEntity entity, String entityPath, Map<String, CdmEntity> entitiesByName, List<FieldViolation> violations) {
        List<CdmForeignKey> foreignKeys = entity.foreignKeys();
        for (int i = 0; i < foreignKeys.size(); i++) {
            CdmForeignKey fk = foreignKeys.get(i);
            String fkPath = entityPath + ".foreignKeys[" + i + "]";

            if (entity.column(fk.columnName()).isEmpty()) {
                violations.add(new FieldViolation(fkPath + ".columnName",
                        "entity '" + entity.name() + "' has no column '" + fk.columnName() + "' for this foreign key"));
                continue;
            }

            CdmEntity referenced = entitiesByName.get(fk.referencesEntity());
            if (referenced == null) {
                violations.add(new FieldViolation(fkPath + ".referencesEntity",
                        "foreign key references unknown entity '" + fk.referencesEntity() + "'"));
                continue;
            }

            if (referenced.column(fk.referencesColumn()).isEmpty()) {
                violations.add(new FieldViolation(fkPath + ".referencesColumn",
                        "foreign key references unknown column '" + fk.referencesColumn()
                                + "' on entity '" + fk.referencesEntity() + "'"));
            }
        }
    }

    private static void validateSeedRows(
            CdmEntity entity, String entityPath, Map<String, CdmEntity> entitiesByName, List<FieldViolation> violations) {
        Set<String> declaredColumnNames = entity.columns().stream()
                .map(CdmColumn::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<CdmColumn> pkColumns = entity.primaryKeyColumns();
        List<List<CdmValue>> seenPkValues = new ArrayList<>();

        List<CdmRow> seedRows = entity.seedRows();
        for (int rowIndex = 0; rowIndex < seedRows.size(); rowIndex++) {
            CdmRow row = seedRows.get(rowIndex);
            String rowPath = entityPath + ".seedRows[" + rowIndex + "]";

            validateRowShape(row, declaredColumnNames, rowPath, entity, violations);
            validateRowValueTypes(row, entity, rowPath, violations);
            validatePrimaryKeyUniqueness(row, pkColumns, entity, rowPath, seenPkValues, violations);
            validateForeignKeyIntegrity(row, entity, entitiesByName, rowPath, violations);
        }
    }

    private static void validateRowShape(
            CdmRow row, Set<String> declaredColumnNames, String rowPath, CdmEntity entity, List<FieldViolation> violations) {
        Set<String> rowKeys = row.values().keySet();
        for (String missing : declaredColumnNames) {
            if (!rowKeys.contains(missing)) {
                violations.add(new FieldViolation(rowPath + "." + missing,
                        "entity '" + entity.name() + "' seed row is missing a value for column '" + missing + "'"));
            }
        }
        for (String extra : rowKeys) {
            if (!declaredColumnNames.contains(extra)) {
                violations.add(new FieldViolation(rowPath + "." + extra,
                        "entity '" + entity.name() + "' seed row has a value for undeclared column '" + extra + "'"));
            }
        }
    }

    private static void validateRowValueTypes(CdmRow row, CdmEntity entity, String rowPath, List<FieldViolation> violations) {
        for (CdmColumn column : entity.columns()) {
            CdmValue value = row.get(column.name());
            if (value instanceof CdmValue.Null) {
                if (!column.nullable()) {
                    violations.add(new FieldViolation(rowPath + "." + column.name(),
                            "column '" + column.name() + "' is not nullable but this row has no value"));
                }
                continue;
            }
            if (!column.type().accepts(value)) {
                violations.add(new FieldViolation(rowPath + "." + column.name(),
                        "expected a " + column.type() + " value for column '" + column.name()
                                + "', got " + value.getClass().getSimpleName()));
            }
        }
    }

    private static void validatePrimaryKeyUniqueness(
            CdmRow row, List<CdmColumn> pkColumns, CdmEntity entity, String rowPath,
            List<List<CdmValue>> seenPkValues, List<FieldViolation> violations) {
        if (pkColumns.isEmpty()) {
            return;
        }
        List<CdmValue> pkValues = pkColumns.stream().map(pk -> row.get(pk.name())).toList();
        boolean duplicate = seenPkValues.stream().anyMatch(existing -> pkTupleEquals(existing, pkValues));
        if (duplicate) {
            violations.add(new FieldViolation(rowPath, "duplicate primary-key value in entity '" + entity.name() + "'"));
        } else {
            seenPkValues.add(pkValues);
        }
    }

    private static void validateForeignKeyIntegrity(
            CdmRow row, CdmEntity entity, Map<String, CdmEntity> entitiesByName, String rowPath, List<FieldViolation> violations) {
        for (CdmForeignKey fk : entity.foreignKeys()) {
            CdmEntity referenced = entitiesByName.get(fk.referencesEntity());
            if (referenced == null) {
                continue; // already flagged by validateForeignKeys
            }
            CdmValue fkValue = row.get(fk.columnName());
            if (fkValue instanceof CdmValue.Null) {
                continue; // a nullable FK column with no value means "no reference", not an error
            }

            boolean matches = referenced.seedRows().stream()
                    .anyMatch(refRow -> CdmValues.equalCanonical(refRow.get(fk.referencesColumn()), fkValue));
            if (!matches) {
                violations.add(new FieldViolation(rowPath + "." + fk.columnName(),
                        "value does not match any '" + fk.referencesEntity() + "." + fk.referencesColumn()
                                + "' seed row"));
            }
        }
    }

    private static boolean pkTupleEquals(List<CdmValue> a, List<CdmValue> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!CdmValues.equalCanonical(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }
}
