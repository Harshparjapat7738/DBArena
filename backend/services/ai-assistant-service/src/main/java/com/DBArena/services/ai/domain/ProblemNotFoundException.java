package com.dbforge.services.ai.domain;

import com.dbforge.common.core.error.NotFoundException;

/** Raised when catalog-service has no published problem for the requested slug. */
public class ProblemNotFoundException extends NotFoundException {

    public ProblemNotFoundException(String slug) {
        super("problem", slug);
    }
}
