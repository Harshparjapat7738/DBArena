package com.DBArena.services.execution.executor;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.ExecutionResult;
import com.DBArena.engine.spi.model.ResultRow;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.engine.spi.model.StatementRequest;
import com.DBArena.services.execution.domain.ExecutionPolicy;
import com.DBArena.services.execution.engine.DatabaseEngine;
import com.DBArena.services.execution.evaluator.CdmValueStringifier;
import com.DBArena.services.execution.evaluator.ResultEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultQueryExecutor implements QueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultQueryExecutor.class);

    /**
     * Matches Postgres's own {@code EXPLAIN ANALYZE} summary line, e.g.
     * {@code "Planning Time: 0.123 ms"} - this parses Postgres's *own*
     * trusted diagnostic output, not the learner's SQL, so hard rule #4
     * (AST-based validation of untrusted input) does not apply to it.
     */
    private static final Pattern PLANNING_TIME_LINE = Pattern.compile("Planning Time:\\s*([0-9.]+)\\s*ms", Pattern.CASE_INSENSITIVE);

    private final DatabaseEngine databaseEngine;
    private final ResultEvaluator resultEvaluator;

    public DefaultQueryExecutor(DatabaseEngine databaseEngine, ResultEvaluator resultEvaluator) {
        this.databaseEngine = databaseEngine;
        this.resultEvaluator = resultEvaluator;
    }

    @Override
    public QueryExecutionOutcome execute(SessionHandle session, EngineType engine, String statementText, ExecutionPolicy policy) {
        // maxResultRows + 1, not maxResultRows: lets ResultEvaluator tell "exactly the limit"
        // apart from "more existed than the limit" (truncated=true) using the same query.
        StatementRequest request = new StatementRequest(
                statementText, policy.statementTimeout(), Optional.of(policy.maxResultRows() + 1));

        ExecutionResult raw = databaseEngine.resolve(engine).execute(session, request);
        if (!raw.isSuccess()) {
            return new QueryExecutionOutcome(raw, Optional.empty());
        }

        Optional<Long> planningTimeMillis = tryCapturePlanningTimeMillis(session, engine, statementText, policy);
        ResultEvaluator.Evaluation evaluation = resultEvaluator.evaluate(raw, policy, planningTimeMillis);
        return new QueryExecutionOutcome(raw, Optional.of(evaluation));
    }

    /**
     * Best-effort only: re-runs the same (already-validated, SELECT-only)
     * statement once more via {@code EXPLAIN ANALYZE} purely to read
     * Postgres's own "Planning Time" line - plain {@code EXPLAIN} never
     * reports it, only {@code ANALYZE} does, and {@code ANALYZE} actually
     * executes the query, which is why this is a deliberate second
     * round-trip rather than free. Bounded by its own short
     * {@link ExecutionPolicy#explainTimeout()}; any failure - timeout,
     * error, unparseable output - yields {@link Optional#empty()} rather
     * than affecting the already-successful primary result in any way.
     */
    private Optional<Long> tryCapturePlanningTimeMillis(
            SessionHandle session, EngineType engine, String statementText, ExecutionPolicy policy) {
        if (engine != EngineType.POSTGRES) {
            return Optional.empty();
        }
        try {
            StatementRequest explainRequest = new StatementRequest(
                    "EXPLAIN (ANALYZE, FORMAT TEXT) " + statementText, policy.explainTimeout());
            ExecutionResult explainResult = databaseEngine.resolve(engine).execute(session, explainRequest);
            if (!explainResult.isSuccess()) {
                return Optional.empty();
            }
            for (ResultRow row : explainResult.rows()) {
                for (var value : row.values()) {
                    String line = CdmValueStringifier.toDisplayString(value);
                    if (line == null) {
                        continue;
                    }
                    Matcher matcher = PLANNING_TIME_LINE.matcher(line);
                    if (matcher.find()) {
                        double millis = Double.parseDouble(matcher.group(1));
                        return Optional.of(Math.round(millis));
                    }
                }
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            log.debug("Could not capture planning time (non-fatal, best-effort only): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
