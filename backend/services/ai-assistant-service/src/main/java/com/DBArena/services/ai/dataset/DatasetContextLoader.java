package com.DBArena.services.ai.dataset;

import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.tools.datasetcli.CdmDatasetLoader;
import com.DBArena.tools.datasetcli.DatasetYamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads a problem's dataset schema (and public sample rows) for the hint
 * context, reusing tools/dataset-cli's {@code CdmDatasetLoader} rather
 * than re-implementing YAML -> CDM parsing. Deliberately tolerant: a
 * missing or unparseable dataset.yaml degrades the hint context (schema
 * section omitted) rather than failing the whole hint request - a
 * catalog problem with a not-yet-authored dataset is a real, expected
 * state (M13's own Carried forward notes {@code datasetSlug} is still an
 * unvalidated loose reference), not a bug in this service.
 */
public class DatasetContextLoader {

    private static final Logger log = LoggerFactory.getLogger(DatasetContextLoader.class);

    private final Path datasetsRoot;

    public DatasetContextLoader(Path datasetsRoot) {
        this.datasetsRoot = datasetsRoot;
    }

    public Optional<CdmDataset> load(String datasetSlug) {
        if (datasetSlug == null || datasetSlug.isBlank()) {
            return Optional.empty();
        }
        Path path = datasetsRoot.resolve(datasetSlug).resolve("dataset.yaml");
        try {
            return Optional.of(CdmDatasetLoader.load(path));
        } catch (IOException e) {
            log.info("no dataset.yaml for '{}' at {} - hint will omit schema context", datasetSlug, path);
            return Optional.empty();
        } catch (DatasetYamlException e) {
            log.warn("dataset.yaml for '{}' at {} did not parse - hint will omit schema context: {}",
                    datasetSlug, path, e.getMessage());
            return Optional.empty();
        }
    }
}
