package com.DBArena.tools.datasetcli.yaml;

import java.util.List;
import java.util.Map;

public record YamlEntity(
        String name,
        List<YamlColumn> columns,
        List<YamlForeignKey> foreignKeys,
        List<Map<String, Object>> seedRows) {
}
