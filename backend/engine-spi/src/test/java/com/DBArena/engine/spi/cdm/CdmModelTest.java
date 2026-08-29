package com.dbforge.engine.spi.cdm;

import com.dbforge.common.core.value.CdmValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CdmModelTest {

    @Test
    void columnRejectsBlankName() {
        assertThatThrownBy(() -> new CdmColumn("", CdmType.INTEGER, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void columnRejectsNullType() {
        assertThatThrownBy(() -> new CdmColumn("id", null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void columnRejectsPrimaryKeyThatIsAlsoNullable() {
        assertThatThrownBy(() -> new CdmColumn("id", CdmType.INTEGER, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void columnAllowsPrimaryKeyNotNullable() {
        CdmColumn column = new CdmColumn("id", CdmType.INTEGER, false, true);
        assertThat(column.primaryKey()).isTrue();
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void entityRejectsBlankName() {
        assertThatThrownBy(() -> new CdmEntity("", List.of(pkColumn()), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entityRejectsNoColumns() {
        assertThatThrownBy(() -> new CdmEntity("t", List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entityColumnLookupFindsByName() {
        CdmEntity entity = new CdmEntity("t", List.of(pkColumn(), valueColumn()), List.of(), List.of());

        assertThat(entity.column("id")).isPresent();
        assertThat(entity.column("nope")).isEmpty();
        assertThat(entity.primaryKeyColumns()).containsExactly(pkColumn());
    }

    @Test
    void datasetRejectsBlankDatasetId() {
        assertThatThrownBy(() -> new CdmDataset("", "Name", 1, List.of(sampleEntity())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datasetRejectsSchemaVersionBelowOne() {
        assertThatThrownBy(() -> new CdmDataset("ds", "Name", 0, List.of(sampleEntity())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datasetRejectsNoEntities() {
        assertThatThrownBy(() -> new CdmDataset("ds", "Name", 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datasetEntityLookupFindsByName() {
        CdmDataset dataset = new CdmDataset("ds", "Name", 1, List.of(sampleEntity()));

        assertThat(dataset.entity("t")).isPresent();
        assertThat(dataset.entity("missing")).isEmpty();
    }

    @Test
    void rowGetReturnsNullInstanceForAnAbsentColumn() {
        CdmRow row = new CdmRow(Map.of("id", new CdmValue.Int(1)));

        assertThat(row.get("id")).isEqualTo(new CdmValue.Int(1));
        assertThat(row.get("missing")).isEqualTo(CdmValue.Null.INSTANCE);
    }

    @Test
    void typeAcceptsItsOwnVariantAndNullButNothingElse() {
        assertThat(CdmType.INTEGER.accepts(new CdmValue.Int(1))).isTrue();
        assertThat(CdmType.INTEGER.accepts(CdmValue.Null.INSTANCE)).isTrue();
        assertThat(CdmType.INTEGER.accepts(new CdmValue.Text("1"))).isFalse();
    }

    @Test
    void foreignKeyRejectsBlankFields() {
        assertThatThrownBy(() -> new CdmForeignKey("", "other", "id")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CdmForeignKey("col", "", "id")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CdmForeignKey("col", "other", "")).isInstanceOf(IllegalArgumentException.class);
    }

    private static CdmColumn pkColumn() {
        return new CdmColumn("id", CdmType.INTEGER, false, true);
    }

    private static CdmColumn valueColumn() {
        return new CdmColumn("value", CdmType.INTEGER, false, false);
    }

    private static CdmEntity sampleEntity() {
        return new CdmEntity("t", List.of(pkColumn()), List.of(), List.of());
    }
}
