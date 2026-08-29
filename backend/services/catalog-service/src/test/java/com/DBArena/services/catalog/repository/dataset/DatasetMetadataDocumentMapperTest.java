package com.DBArena.services.catalog.repository.dataset;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.dataset.DatasetColumn;
import com.DBArena.services.catalog.domain.dataset.DatasetEntity;
import com.DBArena.services.catalog.domain.dataset.DatasetEntityKind;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;
import com.DBArena.services.catalog.domain.dataset.DatasetRelationship;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetMetadataDocumentMapperTest {

    @Test
    void roundTripsEveryField() {
        DatasetMetadata dataset = new DatasetMetadata(
                TypedId.of("01J000DATASET"),
                "two-sum",
                "Two Sum",
                "Employee/order sample data",
                "sample",
                Set.of(EngineType.POSTGRES, EngineType.MONGODB),
                3,
                "~10K rows",
                1,
                1_700_000_000_000L,
                1_700_000_100_000L);

        Document document = DatasetMetadataDocumentMapper.toDocument(dataset);
        DatasetMetadata roundTripped = DatasetMetadataDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(dataset);
    }

    @Test
    void withRevisedContentIncrementsVersion() {
        DatasetMetadata dataset = new DatasetMetadata(
                TypedId.of("01J000DATASET"), "two-sum", "Two Sum", "desc", "sample",
                Set.of(EngineType.POSTGRES), 3, "~10K rows", 1, 1L, 1L);

        DatasetMetadata revised = dataset.withRevisedContent("Two Sum v2", "desc2", "sample",
                Set.of(EngineType.POSTGRES, EngineType.MYSQL), 4, "~20K rows", 2L);

        assertThat(revised.version()).isEqualTo(2);
        assertThat(revised.createdAtEpochMillis()).isEqualTo(1L);
    }

    @Test
    void aDocumentWithNoEntitiesFieldDefaultsToEmpty() {
        Document document = new Document()
                .append("_id", "01J000DATASET2")
                .append("slug", "no-entities-yet")
                .append("name", "Pre-B03 Dataset")
                .append("description", "")
                .append("category", "sample")
                .append("engines", List.of("POSTGRES"))
                .append("entityCount", 0)
                .append("rowCountLabel", "")
                .append("version", 1)
                .append("createdAt", 1L)
                .append("updatedAt", 1L);
        // deliberately no "entities" key - simulates a document written before B03.

        DatasetMetadata roundTripped = DatasetMetadataDocumentMapper.fromDocument(document);

        assertThat(roundTripped.entities()).isEmpty();
    }

    @Test
    void roundTripsEntitiesWithColumnsSampleRowsAndRelationships() {
        DatasetEntity ordersTable = new DatasetEntity(
                "orders",
                DatasetEntityKind.TABLE,
                List.of(
                        new DatasetColumn("id", "INTEGER", false, true, Optional.empty()),
                        new DatasetColumn("customer_id", "INTEGER", false, false, Optional.of("customers.id"))),
                List.of(Map.of("id", "1", "customer_id", "42"), new java.util.LinkedHashMap<>() {{
                    put("id", "2");
                    put("customer_id", null);
                }}),
                List.of(new DatasetRelationship("customers", "many-to-one")));

        DatasetMetadata dataset = new DatasetMetadata(
                TypedId.of("01J000DATASET3"), "sales", "Sales", "desc", "sample",
                Set.of(EngineType.POSTGRES), 1, "~1K rows", 1, 1L, 1L, List.of(ordersTable));

        Document document = DatasetMetadataDocumentMapper.toDocument(dataset);
        DatasetMetadata roundTripped = DatasetMetadataDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(dataset);
        assertThat(roundTripped.entities().get(0).sampleRows().get(1).get("customer_id")).isNull();
    }
}
