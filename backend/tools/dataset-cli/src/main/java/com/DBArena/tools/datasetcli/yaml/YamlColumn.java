package com.DBArena.tools.datasetcli.yaml;

/** {@code type} is a raw string here (e.g. {@code "INTEGER"}) - CdmDatasetLoader resolves it against CdmType, reporting a clear error for a typo rather than an enum-lookup stack trace. */
public record YamlColumn(String name, String type, Boolean nullable, Boolean primaryKey) {
}
