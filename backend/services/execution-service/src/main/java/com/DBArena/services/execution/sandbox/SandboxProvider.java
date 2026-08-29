package com.DBArena.services.execution.sandbox;

import com.DBArena.engine.spi.EngineType;
import com.DBArena.engine.spi.model.SessionHandle;
import com.DBArena.services.execution.domain.ExecutionPolicy;

/**
 * Turns a freshly materialized session into one that is actually safe to
 * point untrusted SQL at, and manages its lifecycle as a leasable
 * resource - hard rule #7 ("the data plane and control plane share no
 * network, no secret, no store") is honored here as far as this
 * environment allows without container/process isolation (B07, not built
 * yet, needs Docker - unavailable in this environment, see CLAUDE.md):
 * every session is a brand-new, disposable database that nothing else ever
 * touches, connected to with a role that has no rights beyond databases it
 * created itself, never the platform's own credentials and never anything
 * client-supplied.
 */
public interface SandboxProvider {

    SessionHandle acquire(EngineType engine, String datasetSlug, ExecutionPolicy policy);

    /** Idempotent - dropping an already-dropped/never-acquired session is a no-op, not an error. */
    void release(SessionHandle session);
}
