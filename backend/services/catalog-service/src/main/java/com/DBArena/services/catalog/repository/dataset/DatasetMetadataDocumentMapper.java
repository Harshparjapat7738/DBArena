package com.DBArena.services.catalog.repository.dataset;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.engine.spi.EngineType;
import com.DBArena.services.catalog.domain.dataset.DatasetColumn;
import com.DBArena.services.catalog.domain.dataset.DatasetEntity;
import com.DBArena.services.catalog.domain.dataset.DatasetEntityKind;
import com.DBArena.services.catalog.domain.dataset.DatasetMetadata;
import com.DBArena.services.catalog.domain.dataset.DatasetRelationship;
import org.bson.Document;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DatasetMetadataDocumentMapper {

    static final String ID = "_id";
    static final String SLUG = "slug";
    static final String NAME = "name";
    static final String DESCRIPTION = "description";
    static final String CATEGORY = "category";
    static final String ENGINES = "engines";
    static final String ENTITY_COUNT = "entityCount";
    static final String ROW_COUNT_LABEL = "rowCountLabel";
    static final String VERSION = "version";
    static final String CREATED_AT = "createdAt";
    static final String UPDATED_AT = "updatedAt";
    static final String ENTITIES = "entities";

    private static final String ENTITY_NAME = "name";
    private static final String ENTITY_KIND = "kind";
    private static final String ENTITY_COLUMNS = "columns";
    private static final String ENTITY_SAMPLE_ROWS = "sampleRows";
    private static final String ENTITY_RELATIONSHIPS = "relationships";

    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_NULLABLE = "nullable";
    private static final String COLUMN_PRIMARY_KEY = "primaryKey";
    private static final String COLUMN_FOREIGN_KEY = "foreignKey";

    private static final String RELATIONSHIP_TO_ENTITY = "toEntity";
    private static final String RELATIONSHIP_TYPE = "type";

    private DatasetMetadataDocumentMapper() {
    }

    public static Document toDocument(DatasetMetadata dataset) {
        return new Document()
                .append(ID, dataset.id().value())
                .append(SLUG, dataset.slug())
                .append(NAME, dataset.name())
                .append(DESCRIPTION, dataset.description())
                .append(CATEGORY, dataset.category())
                .append(ENGINES, dataset.engines().stream().map(Enum::name).toList())
                .append(ENTITY_COUNT, dataset.entityCount())
                .append(ROW_COUNT_LABEL, dataset.rowCountLabel())
                .append(VERSION, dataset.version())
                .append(CREATED_AT, dataset.createdAtEpochMillis())
                .append(UPDATED_AT, dataset.updatedAtEpochMillis())
                .append(ENTITIES, dataset.entities().stream().map(DatasetMetadataDocumentMapper::entityToDocument).toList());
    }

    public static DatasetMetadata fromDocument(Document document) {
        Set<EngineType> engines = document.getList(ENGINES, String.class, List.of()).stream()
                .map(EngineType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DatasetEntity> entities = document.getList(ENTITIES, Document.class, List.of()).stream()
                .map(DatasetMetadataDocumentMapper::entityFromDocument)
                .toList();

        return new DatasetMetadata(
                TypedId.of(document.getString(ID)),
                document.getString(SLUG),
                document.getString(NAME),
                document.getString(DESCRIPTION),
                document.getString(CATEGORY),
                engines,
                document.getInteger(ENTITY_COUNT, 0),
                document.getString(ROW_COUNT_LABEL),
                document.getInteger(VERSION, 1),
                document.getLong(CREATED_AT),
                document.getLong(UPDATED_AT),
                entities);
    }

    private static Document entityToDocument(DatasetEntity entity) {
        return new Document()
                .append(ENTITY_NAME, entity.name())
                .append(ENTITY_KIND, entity.kind().name())
                .append(ENTITY_COLUMNS, entity.columns().stream().map(DatasetMetadataDocumentMapper::columnToDocument).toList())
                .append(ENTITY_SAMPLE_ROWS, entity.sampleRows().stream().map(row -> {
                    Document rowDocument = new Document();
                    rowDocument.putAll(row);
                    return rowDocument;
                }).toList())
                .append(ENTITY_RELATIONSHIPS, entity.relationships().stream().map(DatasetMetadataDocumentMapper::relationshipToDocument).toList());
    }

    private static DatasetEntity entityFromDocument(Document document) {
        List<DatasetColumn> columns = document.getList(ENTITY_COLUMNS, Document.class, List.of()).stream()
                .map(DatasetMetadataDocumentMapper::columnFromDocument)
                .toList();
        List<Map<String, String>> sampleRows = document.getList(ENTITY_SAMPLE_ROWS, Document.class, List.of()).stream()
                .map(row -> {
                    Map<String, String> plain = new LinkedHashMap<>();
                    for (var entry : row.entrySet()) {
                        plain.put(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
                    }
                    return plain;
                })
                .toList();
        List<DatasetRelationship> relationships = document.getList(ENTITY_RELATIONSHIPS, Document.class, List.of()).stream()
                .map(DatasetMetadataDocumentMapper::relationshipFromDocument)
                .toList();

        return new DatasetEntity(
                document.getString(ENTITY_NAME),
                DatasetEntityKind.valueOf(document.getString(ENTITY_KIND)),
                columns,
                sampleRows,
                relationships);
    }

    private static Document columnToDocument(DatasetColumn column) {
        return new Document()
                .append(COLUMN_NAME, column.name())
                .append(COLUMN_TYPE, column.type())
                .append(COLUMN_NULLABLE, column.nullable())
                .append(COLUMN_PRIMARY_KEY, column.primaryKey())
                .append(COLUMN_FOREIGN_KEY, column.foreignKey().orElse(null));
    }

    private static DatasetColumn columnFromDocument(Document document) {
        return new DatasetColumn(
                document.getString(COLUMN_NAME),
                document.getString(COLUMN_TYPE),
                Boolean.TRUE.equals(document.getBoolean(COLUMN_NULLABLE)),
                Boolean.TRUE.equals(document.getBoolean(COLUMN_PRIMARY_KEY)),
                Optional.ofNullable(document.getString(COLUMN_FOREIGN_KEY)));
    }

    private static Document relationshipToDocument(DatasetRelationship relationship) {
        return new Document()
                .append(RELATIONSHIP_TO_ENTITY, relationship.toEntity())
                .append(RELATIONSHIP_TYPE, relationship.type());
    }

    private static DatasetRelationship relationshipFromDocument(Document document) {
        return new DatasetRelationship(document.getString(RELATIONSHIP_TO_ENTITY), document.getString(RELATIONSHIP_TYPE));
    }
}
