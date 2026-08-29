package com.DBArena.services.execution.executor;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.domain.ExecutionPolicy;

/** Runs one already-validated statement against an already-sandboxed session and evaluates its result - the "execution" + "metrics" steps of the request flow. */
public interface QueryExecutor {

    QueryExecutionOutcome execute(SessionHandle session, EngineType engine, String statementText, ExecutionPolicy policy);
}
