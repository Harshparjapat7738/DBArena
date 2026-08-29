package com.DBArena.tools.datasetcli;

import com.DBArena.common.core.value.CdmValue;
import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.engine.spi.cdm.CdmEntity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CdmDatasetLoaderTest {

    private static InputStream yaml(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadsEveryCdmTypeVariantFromYamlScalars() throws IOException {
        String source = """
                datasetId: t1
                name: Test One
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                      - name: flag
                        type: BOOLEAN
                        nullable: false
                      - name: price
                        type: DECIMAL
                        nullable: false
                      - name: label
                        type: TEXT
                        nullable: false
                      - name: seen_at
                        type: TIMESTAMP
                        nullable: false
                      - name: extra
                        type: JSON
                        nullable: true
                    seedRows:
                      - id: 1
                        flag: true
                        price: 1.50
                        label: hello
                        seen_at: "2026-01-01T00:00:00Z"
                        extra:
                          nested: value
                """;

        CdmDataset dataset = CdmDatasetLoader.load(yaml(source));

        assertThat(dataset.datasetId()).isEqualTo("t1");
        CdmEntity entity = dataset.entity("things").orElseThrow();
        var row = entity.seedRows().get(0);

        assertThat(row.get("id")).isEqualTo(new CdmValue.Int(1));
        assertThat(row.get("flag")).isEqualTo(new CdmValue.Bool(true));
        assertThat(((CdmValue.Decimal) row.get("price")).toBigDecimal()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(row.get("label")).isEqualTo(new CdmValue.Text("hello"));
        assertThat(row.get("seen_at")).isEqualTo(new CdmValue.Timestamp(1767225600000L));
        assertThat(row.get("extra")).isInstanceOf(CdmValue.Json.class);
        assertThat(((CdmValue.Json) row.get("extra")).canonicalJson()).contains("\"nested\"").contains("\"value\"");
    }

    @Test
    void aNullSeedValueBecomesCdmValueNull() throws IOException {
        String source = """
                datasetId: t2
                name: Test Two
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                      - name: label
                        type: TEXT
                        nullable: true
                    seedRows:
                      - id: 1
                        label: null
                """;

        CdmDataset dataset = CdmDatasetLoader.load(yaml(source));

        assertThat(dataset.entity("things").orElseThrow().seedRows().get(0).get("label"))
                .isEqualTo(CdmValue.Null.INSTANCE);
    }

    @Test
    void anUnknownColumnTypeNameFailsWithAClearMessage() {
        String source = """
                datasetId: t3
                name: Test Three
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: NOT_A_REAL_TYPE
                        nullable: false
                        primaryKey: true
                """;

        assertThatThrownBy(() -> CdmDatasetLoader.load(yaml(source)))
                .isInstanceOf(DatasetYamlException.class)
                .hasMessageContaining("NOT_A_REAL_TYPE");
    }

    @Test
    void aMalformedTimestampFailsWithAClearMessage() {
        String source = """
                datasetId: t4
                name: Test Four
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                      - name: seen_at
                        type: TIMESTAMP
                        nullable: false
                    seedRows:
                      - id: 1
                        seen_at: "not a timestamp"
                """;

        assertThatThrownBy(() -> CdmDatasetLoader.load(yaml(source)))
                .isInstanceOf(DatasetYamlException.class)
                .hasMessageContaining("ISO-8601");
    }

    @Test
    void aWrongScalarKindForTheDeclaredTypeFailsWithAClearMessage() {
        String source = """
                datasetId: t5
                name: Test Five
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                    seedRows:
                      - id: "not an integer"
                """;

        assertThatThrownBy(() -> CdmDatasetLoader.load(yaml(source)))
                .isInstanceOf(DatasetYamlException.class)
                .hasMessageContaining("expected an integer");
    }

    @Test
    void aDecimalLiteralForAnIntegerColumnIsRejectedRatherThanSilentlyTruncated() {
        String source = """
                datasetId: t8
                name: Test Eight
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                    seedRows:
                      - id: 1.5
                """;

        assertThatThrownBy(() -> CdmDatasetLoader.load(yaml(source)))
                .isInstanceOf(DatasetYamlException.class)
                .hasMessageContaining("expected an integer");
    }

    @Test
    void anEmptyFileFails() {
        assertThatThrownBy(() -> CdmDatasetLoader.load(yaml("")))
                .isInstanceOf(DatasetYamlException.class);
    }

    @Test
    void anEntityWithNoColumnsFailsAtLoadTimeNotJustValidationTime() {
        String source = """
                datasetId: t6
                name: Test Six
                schemaVersion: 1
                entities:
                  - name: things
                    columns: []
                """;

        assertThatThrownBy(() -> CdmDatasetLoader.load(yaml(source)))
                .isInstanceOf(DatasetYamlException.class)
                .hasMessageContaining("at least one column");
    }

    /** A column not declared on the entity is left for CdmDatasetValidator to report, not this loader. */
    @Test
    void anUndeclaredSeedColumnIsSilentlyDroppedByTheLoaderNotRejected() throws IOException {
        String source = """
                datasetId: t7
                name: Test Seven
                schemaVersion: 1
                entities:
                  - name: things
                    columns:
                      - name: id
                        type: INTEGER
                        nullable: false
                        primaryKey: true
                    seedRows:
                      - id: 1
                        surprise: 42
                """;

        CdmDataset dataset = CdmDatasetLoader.load(yaml(source));

        assertThat(dataset.entity("things").orElseThrow().seedRows().get(0).values()).doesNotContainKey("surprise");
    }
}
