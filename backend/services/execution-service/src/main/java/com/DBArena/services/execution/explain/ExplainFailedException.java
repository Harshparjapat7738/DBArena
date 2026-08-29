package com.DBArena.services.execution.explain;

import com.DBArena.common.core.error.DomainException;

import java.util.Map;

/** The engine could not produce a plan (a genuine engine-level failure, distinct from validation rejection, which never reaches this far). */
public class ExplainFailedException extends DomainException {

    public ExplainFailedException(String message) {
        super("execution.explain_failed", 422, message, Map.of());
    }
}
