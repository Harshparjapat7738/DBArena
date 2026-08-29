package com.DBArena.services.execution.service;

import com.DBArena.common.core.error.NotFoundException;

/**
 * Also thrown for a real execution owned by a different user - a caller
 * must never learn "this id exists, just isn't yours" (403) vs "this id
 * doesn't exist" (404); both look identical from the outside, same
 * least-information-leakage posture as "no hidden test-case exposure"
 * generally.
 */
public class ExecutionNotFoundException extends NotFoundException {

    public ExecutionNotFoundException(String id) {
        super("Execution", id);
    }
}
