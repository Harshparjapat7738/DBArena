package com.DBArena.tools.datasetcli;

import com.DBArena.common.core.value.CdmValue;
import com.DBArena.engine.spi.cdm.CdmColumn;
import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.engine.spi.cdm.CdmEntity;
import com.DBArena.engine.spi.cdm.CdmForeignKey;
import com.DBArena.engine.spi.cdm.CdmRow;
import com.DBArena.engine.spi.cdm.CdmType;
import com.DBArena.tools.datasetcli.yaml.YamlColumn;
import com.DBArena.tools.datasetcli.yaml.YamlDataset;
import com.DBArena.tools.datasetcli.yaml.YamlEntity;
import com.DBArena.tools.datasetcli.yaml.YamlForeignKey;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts an authored dataset.yaml into the engine-spi CDM model. This is
 * deliberately kept separate from the CDM model itself (engine-spi) and
 * from {@code CdmDatasetValidator} - YAML is one authoring format among
 * possible future others (B18's ingestion-service may need to load a
 * dataset from CSV or a spreadsheet export), so the "how do we read a file
 * off disk" concern stays out of the framework-free, format-agnostic model.
 *
 * <p>Two separate Jackson mappers, deliberately not shared: {@link #YAML}
 * parses the source file; {@link #JSON} only re-serializes an already-parsed
 * fragment into the canonical text a {@code CdmValue.Json} column stores -
 * reusing the YAML mapper for that would risk YAML-specific formatting
 * (e.g. unquoted keys) leaking into what's supposed to be canonical JSON.
 */
public final class CdmDatasetLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final ObjectMapper JSON = new ObjectMapper();

    private CdmDatasetLoader() {
    }

    public static CdmDataset load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        }
    }

    public static CdmDataset load(InputStream yamlContent) throws IOException {
        YamlDataset raw;
        try {
            raw = YAML.readValue(yamlContent, YamlDataset.class);
        } catch (IOException e) {
            throw new DatasetYamlException("could not parse YAML: " + e.getMessage(), e);
        }
        if (raw == null) {
            throw new DatasetYamlException("dataset.yaml is empty");
        }
        return toCdmDataset(raw);
    }

    private static CdmDataset toCdmDataset(YamlDataset raw) {
        if (raw.entities() == null || raw.entities().isEmpty()) {
            throw new DatasetYamlException("dataset must declare at least one entity");
        }
        List<CdmEntity> entities = raw.entities().stream()
                .map(CdmDatasetLoader::toCdmEntity)
                .toList();
        int schemaVersion = raw.schemaVersion() == null ? 0 : raw.schemaVersion();
        return new CdmDataset(raw.datasetId(), raw.name(), schemaVersion, entities);
    }

    private static CdmEntity toCdmEntity(YamlEntity raw) {
        if (raw.name() == null || raw.name().isBlank()) {
            throw new DatasetYamlException("an entity is missing its 'name'");
        }
        if (raw.columns() == null || raw.columns().isEmpty()) {
            throw new DatasetYamlException("entity '" + raw.name() + "' must declare at least one column");
        }

        List<CdmColumn> columns = raw.columns().stream()
                .map(column -> toCdmColumn(column, raw.name()))
                .toList();
        Map<String, CdmColumn> columnsByName = new LinkedHashMap<>();
        for (CdmColumn column : columns) {
            columnsByName.put(column.name(), column);
        }

        List<CdmForeignKey> foreignKeys = raw.foreignKeys() == null
                ? List.of()
                : raw.foreignKeys().stream().map(CdmDatasetLoader::toCdmForeignKey).toList();

        List<CdmRow> seedRows = raw.seedRows() == null
                ? List.of()
                : raw.seedRows().stream()
                        .map(row -> toCdmRow(row, columnsByName, raw.name()))
                        .toList();

        return new CdmEntity(raw.name(), columns, foreignKeys, seedRows);
    }

    private static CdmColumn toCdmColumn(YamlColumn raw, String entityName) {
        if (raw.name() == null || raw.name().isBlank()) {
            throw new DatasetYamlException("entity '" + entityName + "' has a column with no 'name'");
        }
        if (raw.type() == null || raw.type().isBlank()) {
            throw new DatasetYamlException("entity '" + entityName + "' column '" + raw.name() + "' has no 'type'");
        }
        CdmType type;
        try {
            type = CdmType.valueOf(raw.type().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatasetYamlException("entity '" + entityName + "' column '" + raw.name()
                    + "' has unknown type '" + raw.type() + "' - expected one of " + List.of(CdmType.values()));
        }
        boolean nullable = Boolean.TRUE.equals(raw.nullable());
        boolean primaryKey = Boolean.TRUE.equals(raw.primaryKey());
        return new CdmColumn(raw.name(), type, nullable, primaryKey);
    }

    private static CdmForeignKey toCdmForeignKey(YamlForeignKey raw) {
        return new CdmForeignKey(raw.columnName(), raw.referencesEntity(), raw.referencesColumn());
    }

    private static CdmRow toCdmRow(Map<String, Object> raw, Map<String, CdmColumn> columnsByName, String entityName) {
        Map<String, CdmValue> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String columnName = entry.getKey();
            CdmColumn column = columnsByName.get(columnName);
            String path = "entity '" + entityName + "' seed row, column '" + columnName + "'";
            if (column == null) {
                // Not a loader-level error - CdmDatasetValidator reports "value for
                // undeclared column" with a proper FieldViolation and a row index;
                // failing to convert an unknown column's value here would hide that
                // behind a less useful parse error instead.
                continue;
            }
            values.put(columnName, toCdmValue(entry.getValue(), column, path));
        }
        return new CdmRow(values);
    }

    private static CdmValue toCdmValue(Object raw, CdmColumn column, String path) {
        if (raw == null) {
            return CdmValue.Null.INSTANCE;
        }
        return switch (column.type()) {
            case BOOLEAN -> {
                if (raw instanceof Boolean b) {
                    yield new CdmValue.Bool(b);
                }
                throw new DatasetYamlException(path + ": expected a boolean, got '" + raw + "'");
            }
            case INTEGER -> {
                // Deliberately not "any Number": with USE_BIG_DECIMAL_FOR_FLOATS
                // enabled, a mistyped "1.5" parses as BigDecimal, which IS-A
                // Number - accepting any Number here would silently truncate it
                // to 1 via longValue() instead of rejecting it.
                if (raw instanceof Integer || raw instanceof Long || raw instanceof Short
                        || raw instanceof java.math.BigInteger) {
                    yield new CdmValue.Int(((Number) raw).longValue());
                }
                throw new DatasetYamlException(path + ": expected an integer, got '" + raw + "'");
            }
            case DECIMAL -> {
                if (raw instanceof BigDecimal bd) {
                    yield CdmValue.Decimal.of(bd);
                }
                if (raw instanceof Number n) {
                    yield CdmValue.Decimal.of(new BigDecimal(n.toString()));
                }
                throw new DatasetYamlException(path + ": expected a decimal, got '" + raw + "'");
            }
            case TEXT -> {
                if (raw instanceof String s) {
                    yield new CdmValue.Text(s);
                }
                throw new DatasetYamlException(path + ": expected text, got '" + raw + "'");
            }
            case TIMESTAMP -> {
                if (raw instanceof String s) {
                    try {
                        yield new CdmValue.Timestamp(Instant.parse(s).toEpochMilli());
                    } catch (DateTimeParseException e) {
                        throw new DatasetYamlException(path + ": '" + s + "' is not an ISO-8601 instant "
                                + "(e.g. '2026-01-01T00:00:00Z')", e);
                    }
                }
                throw new DatasetYamlException(path + ": expected an ISO-8601 timestamp string, got '" + raw + "'");
            }
            case JSON -> {
                try {
                    JsonNode node = YAML.convertValue(raw, JsonNode.class);
                    yield new CdmValue.Json(JSON.writeValueAsString(node));
                } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new DatasetYamlException(path + ": could not canonicalize as JSON", e);
                }
            }
        };
    }
}
