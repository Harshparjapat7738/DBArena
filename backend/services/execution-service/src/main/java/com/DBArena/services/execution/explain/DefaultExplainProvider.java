package com.DBArena.services.execution.explain;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ExplainPlan;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.engine.spi.model.StatementRequest;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.engine.DatabaseEngine;
import org.springframework.stereotype.Component;

@Component
public class DefaultExplainProvider implements ExplainProvider {

    private final DatabaseEngine databaseEngine;

    public DefaultExplainProvider(DatabaseEngine databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    @Override
    public ExplainPlan explain(SessionHandle session, EngineType engine, String statementText, ExecutionPolicy policy) {
        StatementRequest request = new StatementRequest(statementText, policy.explainTimeout());
        try {
            return databaseEngine.resolve(engine).explain(session, request);
        } catch (RuntimeException e) {
            // adapter-postgres's explain() throws on failure (unlike execute(), which returns a
            // failure result) - translate to a clean domain exception rather than a raw 500 with
            // the adapter's internal exception message/stack leaking through.
            throw new ExplainFailedException("Could not produce an execution plan for this statement");
        }
    }
}
