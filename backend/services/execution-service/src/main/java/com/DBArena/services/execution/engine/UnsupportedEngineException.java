package com.DBArena.services.execution.engine;

import com.DBArena.common.core.error.DomainException;
import com.DBArena.engine.spi.EngineType;

import java.util.Map;

/** Raised for an {@link EngineType} with no registered {@link com.DBArena.engine.spi.DatabaseEngineAdapter} bean yet (MySQL, MongoDB - B05/MySQL wiring not done in this pass). */
public class UnsupportedEngineException extends DomainException {

    public UnsupportedEngineException(EngineType type) {
        super("execution.unsupported_engine", 422, type + " is not yet supported for execution", Map.of("engine", type.name()));
    }
}
