package com.dbforge.engine.spi;

import com.dbforge.engine.spi.cdm.CdmDataset;
import com.dbforge.engine.spi.model.ExecutionResult;
import com.dbforge.engine.spi.model.ExplainPlan;
import com.dbforge.engine.spi.model.MaterializationResult;
import com.dbforge.engine.spi.model.SchemaSnapshot;
import com.dbforge.engine.spi.model.SessionHandle;
import com.dbforge.engine.spi.model.StatementRequest;

/**
 * Everything a database engine must be able to do to host a learner's
 * practice session. Adding a new engine means implementing this plus a
 * {@code Materializer}, a {@code StatementAnalyzer}, a {@code PlanParser},
 * and a total {@code TypeMapper} - see backend/CLAUDE.md "Adding a new
 * engine". If adding an engine requires touching anything outside
 * {@code engine-adapters/<new-engine>}, the abstraction has leaked; stop
 * and flag it rather than reaching into execution-service or
 * submission-service.
 *
 * <p>Implementations are per-engine singletons (one Spring bean per
 * adapter in the service that hosts it) - not per-session. Session state
 * lives in {@link SessionHandle}, not in the adapter instance.
 */
public interface DatabaseEngineAdapter {

    EngineType engineType();

    /**
     * Materializes a fresh copy of {@code dataset} into a new session.
     * For Postgres this is expected to be a cheap template-database clone
     * after the first materialization (see {@link #templateClone}); for
     * Mongo, document shaping happens here. {@code dataset} is expected to
     * already be valid - see {@link com.dbforge.engine.spi.cdm.CdmDatasetValidator}
     * - an adapter is not required to re-validate it.
     */
    MaterializationResult materialize(CdmDataset dataset);

    /**
     * Clones an already-materialized template session into a new,
     * independent one - the fast path for starting a learner session
     * against a dataset that's been materialized before. An adapter for
     * which cloning has no cheaper implementation than
     * {@link #materialize} may implement this by delegating to it.
     */
    SessionHandle templateClone(SessionHandle templateSession);

    /** Reads back the live schema of a session - what tables/collections and columns actually exist right now. */
    SchemaSnapshot introspect(SessionHandle session);

    /**
     * Runs one already-validated statement against a session and returns
     * its result set or the engine's error, never throwing for a normal
     * query failure (a syntax error in the learner's own query is data,
     * not an exceptional condition here).
     */
    ExecutionResult execute(SessionHandle session, StatementRequest request);

    /** Produces the engine's native query plan for a statement without executing its side effects where the engine supports that. */
    ExplainPlan explain(SessionHandle session, StatementRequest request);

    /** Releases whatever resources {@code session} holds (a cloned database, a connection pool entry). Idempotent. */
    void release(SessionHandle session);
}
