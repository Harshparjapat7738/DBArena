package com.DBArena.engine.spi.cdm;

import com.DBArena.common.core.value.CdmValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CdmDatasetValidatorTest {

    private static CdmColumn col(String name, CdmType type, boolean nullable, boolean pk) {
        return new CdmColumn(name, type, nullable, pk);
    }

    private static CdmRow row(Object... nameValuePairs) {
        Map<String, CdmValue> values = new java.util.LinkedHashMap<>();
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            values.put((String) nameValuePairs[i], (CdmValue) nameValuePairs[i + 1]);
        }
        return new CdmRow(values);
    }

    private static CdmEntity numbersEntity(List<CdmRow> seedRows) {
        return new CdmEntity("numbers",
                List.of(
                        col("id", CdmType.INTEGER, false, true),
                        col("value", CdmType.INTEGER, false, false)),
                List.of(),
                seedRows);
    }

    private static CdmEntity validNumbersEntity() {
        return numbersEntity(List.of(
                row("id", new CdmValue.Int(1), "value", new CdmValue.Int(2)),
                row("id", new CdmValue.Int(2), "value", new CdmValue.Int(7))));
    }

    private static CdmDataset validDataset() {
        return new CdmDataset("ds", "Dataset", 1, List.of(validNumbersEntity()));
    }

    @Test
    void aWellFormedDatasetHasNoViolations() {
        CdmValidationResult result = CdmDatasetValidator.validate(validDataset());

        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void duplicateEntityNamesAreCaseInsensitivelyRejected() {
        CdmEntity a = numbersEntity(List.of(row("id", new CdmValue.Int(1), "value", new CdmValue.Int(1))));
        CdmEntity b = new CdmEntity("Numbers", a.columns(), List.of(),
                List.of(row("id", new CdmValue.Int(2), "value", new CdmValue.Int(2))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(a, b));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[1].name"));
    }

    @Test
    void duplicateColumnNamesAreCaseInsensitivelyRejected() {
        CdmEntity entity = new CdmEntity("t",
                List.of(col("id", CdmType.INTEGER, false, true), col("ID", CdmType.TEXT, true, false)),
                List.of(), List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[0].columns[1].name"));
    }

    @Test
    void entityWithNoPrimaryKeyIsRejected() {
        CdmEntity entity = new CdmEntity("t", List.of(col("value", CdmType.INTEGER, false, false)), List.of(), List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.message()).contains("primary-key"));
    }

    @Test
    void foreignKeyToAnUndeclaredOwnColumnIsRejected() {
        CdmEntity entity = new CdmEntity("t", List.of(col("id", CdmType.INTEGER, false, true)),
                List.of(new CdmForeignKey("missing_col", "t", "id")), List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[0].foreignKeys[0].columnName"));
    }

    @Test
    void foreignKeyToAnUnknownEntityIsRejected() {
        CdmEntity entity = new CdmEntity("t",
                List.of(col("id", CdmType.INTEGER, false, true), col("ref", CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey("ref", "nonexistent", "id")), List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[0].foreignKeys[0].referencesEntity"));
    }

    @Test
    void foreignKeyToAnUnknownColumnOnAKnownEntityIsRejected() {
        CdmEntity referenced = numbersEntity(List.of());
        CdmEntity referencing = new CdmEntity("t",
                List.of(col("id", CdmType.INTEGER, false, true), col("ref", CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey("ref", "numbers", "nonexistent_column")), List.of());
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(referenced, referencing));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[1].foreignKeys[0].referencesColumn"));
    }

    @Test
    void seedRowMissingAColumnIsRejected() {
        CdmEntity entity = numbersEntity(List.of(row("id", new CdmValue.Int(1))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[0].seedRows[0].value"));
    }

    @Test
    void seedRowWithAnUndeclaredColumnIsRejected() {
        CdmEntity entity = numbersEntity(List.of(
                row("id", new CdmValue.Int(1), "value", new CdmValue.Int(2), "extra", new CdmValue.Text("x"))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[0].seedRows[0].extra"));
    }

    @Test
    void nonNullableColumnWithANullSeedValueIsRejected() {
        CdmEntity entity = numbersEntity(List.of(row("id", new CdmValue.Int(1), "value", CdmValue.Null.INSTANCE)));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v ->
                assertThat(v.field()).isEqualTo("entities[0].seedRows[0].value"));
    }

    @Test
    void aSeedValueOfTheWrongVariantIsRejected() {
        CdmEntity entity = numbersEntity(List.of(row("id", new CdmValue.Int(1), "value", new CdmValue.Text("not a number"))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.message()).contains("expected a INTEGER"));
    }

    @Test
    void duplicatePrimaryKeyValuesAreRejected() {
        CdmEntity entity = numbersEntity(List.of(
                row("id", new CdmValue.Int(1), "value", new CdmValue.Int(2)),
                row("id", new CdmValue.Int(1), "value", new CdmValue.Int(99))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(entity));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.message()).contains("duplicate primary-key"));
    }

    @Test
    void foreignKeySeedValueThatDoesNotMatchAnyReferencedRowIsRejected() {
        CdmEntity referenced = validNumbersEntity(); // has ids 1 and 2
        CdmEntity referencing = new CdmEntity("queries",
                List.of(col("id", CdmType.INTEGER, false, true), col("number_ref", CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey("number_ref", "numbers", "id")),
                List.of(row("id", new CdmValue.Int(1), "number_ref", new CdmValue.Int(999))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(referenced, referencing));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.violations()).anySatisfy(v -> assertThat(v.field()).isEqualTo("entities[1].seedRows[0].number_ref"));
    }

    @Test
    void aNullableForeignKeyColumnMayBeNullWithoutAViolation() {
        CdmEntity referenced = validNumbersEntity();
        CdmEntity referencing = new CdmEntity("queries",
                List.of(col("id", CdmType.INTEGER, false, true), col("number_ref", CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey("number_ref", "numbers", "id")),
                List.of(row("id", new CdmValue.Int(1), "number_ref", CdmValue.Null.INSTANCE)));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(referenced, referencing));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void foreignKeySeedValueThatMatchesAReferencedRowPasses() {
        CdmEntity referenced = validNumbersEntity();
        CdmEntity referencing = new CdmEntity("queries",
                List.of(col("id", CdmType.INTEGER, false, true), col("number_ref", CdmType.INTEGER, true, false)),
                List.of(new CdmForeignKey("number_ref", "numbers", "id")),
                List.of(row("id", new CdmValue.Int(1), "number_ref", new CdmValue.Int(2))));
        CdmDataset dataset = new CdmDataset("ds", "Dataset", 1, List.of(referenced, referencing));

        CdmValidationResult result = CdmDatasetValidator.validate(dataset);

        assertThat(result.valid()).isTrue();
    }
}
