package com.DBArena.services.execution.validation;

import com.DBArena.engine.spi.EngineType;

/** Same registry-over-a-list-of-beans pattern as {@code DatabaseEngine} - register a new {@link QueryValidator} bean for a new engine, nothing here changes. */
public interface QueryValidatorRegistry {

    QueryValidator resolve(EngineType type);
}
