package com.dbforge.tools.datasetcli.yaml;

import java.util.List;

/** The raw shape Jackson binds a dataset.yaml file into, before {@code CdmDatasetLoader} converts it to a {@code CdmDataset}. */
public record YamlDataset(String datasetId, String name, Integer schemaVersion, List<YamlEntity> entities) {
}
