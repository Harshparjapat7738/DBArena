package com.DBArena.services.execution.materializer;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.SessionHandle;

/**
 * Turns a dataset slug into a ready-to-query session. First call for a
 * given {@code (engine, datasetSlug)} pair materializes a template from the
 * CDM descriptor on disk (slow, real DDL+inserts); every call after that -
 * for this or any other learner - clones the cached template (fast), per
 * {@link com.DBArena.engine.spi.DatabaseEngineAdapter#templateClone}'s own
 * contract. Never hands out the template itself to a caller - only clones.
 */
public interface DatasetMaterializer {

    SessionHandle acquireFreshSession(EngineType engine, String datasetSlug);
}
