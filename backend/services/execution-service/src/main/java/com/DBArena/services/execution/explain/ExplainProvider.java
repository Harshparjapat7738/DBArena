package com.DBArena.services.execution.explain;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ExplainPlan;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.domain.ExecutionPolicy;

/** Backs {@code POST /api/v1/executions/{id}/explain} - always re-validates the statement first (the same {@code QueryValidator} gate execution itself goes through), never explains a statement that would have been rejected outright. */
public interface ExplainProvider {

    ExplainPlan explain(SessionHandle session, EngineType engine, String statementText, ExecutionPolicy policy);
}
