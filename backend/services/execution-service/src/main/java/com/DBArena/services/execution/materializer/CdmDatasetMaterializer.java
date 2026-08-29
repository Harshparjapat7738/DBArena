package com.DBArena.services.execution.materializer;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.config.ExecutionProperties;
import com.DBArena.services.execution.engine.DatabaseEngine;
import com.DBArena.tools.datasetcli.CdmDatasetLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Template sessions are cached in-memory, per {@code (engine, datasetSlug)},
 * for the life of this process - re-materializing on every request would
 * make every execution pay the slow path. Loading the CDM descriptor
 * itself is also cached, separately, since a dataset's on-disk YAML never
 * changes without a restart in this milestone (no hot-reload; datasets/
 * versioning at runtime is future work).
 */
@Component
public class CdmDatasetMaterializer implements DatasetMaterializer {

    private final DatabaseEngine databaseEngine;
    private final Path datasetsRoot;
    private final ConcurrentHashMap<String, CdmDataset> datasetCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionHandle> templateCache = new ConcurrentHashMap<>();

    public CdmDatasetMaterializer(DatabaseEngine databaseEngine, ExecutionProperties properties) {
        this.databaseEngine = databaseEngine;
        this.datasetsRoot = Path.of(properties.getDatasetsRoot());
    }

    @Override
    public SessionHandle acquireFreshSession(EngineType engine, String datasetSlug) {
        String cacheKey = engine.name() + ":" + datasetSlug;
        SessionHandle template = templateCache.computeIfAbsent(cacheKey, key -> materializeTemplate(engine, datasetSlug));
        return databaseEngine.resolve(engine).templateClone(template);
    }

    private SessionHandle materializeTemplate(EngineType engine, String datasetSlug) {
        CdmDataset dataset = datasetCache.computeIfAbsent(datasetSlug, slug -> loadFromDisk(slug));
        return databaseEngine.resolve(engine).materialize(dataset).session();
    }

    private CdmDataset loadFromDisk(String datasetSlug) {
        Path descriptor = datasetsRoot.resolve(datasetSlug).resolve("dataset.yaml");
        try {
            return CdmDatasetLoader.load(descriptor);
        } catch (IOException e) {
            throw new DatasetLoadException(datasetSlug, e.getMessage());
        } catch (RuntimeException e) {
            // CdmDatasetLoader.DatasetYamlException and friends - a malformed descriptor is this
            // dataset's problem, not an execution-service bug, so it maps to the same 404-ish
            // "not available" outcome as a missing file rather than a raw 500.
            throw new DatasetLoadException(datasetSlug, e.getMessage());
        }
    }
}
