package com.dbforge.tools.datasetcli.yaml;

public record YamlForeignKey(String columnName, String referencesEntity, String referencesColumn) {
}
