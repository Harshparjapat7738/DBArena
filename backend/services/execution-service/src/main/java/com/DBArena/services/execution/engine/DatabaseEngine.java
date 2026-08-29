package com.DBArena.services.execution.engine;

import com.DBArena.engine.spi.DatabaseEngineAdapter;
import com.DBArena.engine.spi.EngineType;

/**
 * execution-service's own registry over engine-spi's
 * {@link DatabaseEngineAdapter} - the actual per-engine capability contract
 * (materialize/clone/execute/explain/release) already lives there and is
 * not re-declared here (see backend/CLAUDE.md "Adding a new engine"; a
 * second copy of that interface would be exactly the duplication root
 * CLAUDE.md warns against). This is just how execution-service looks one up
 * by {@link EngineType} - keeping MySQL/Mongo extensibility intact means
 * registering another {@code DatabaseEngineAdapter} bean, not touching this
 * interface or any of its callers.
 */
public interface DatabaseEngine {

    DatabaseEngineAdapter resolve(EngineType type);
}
