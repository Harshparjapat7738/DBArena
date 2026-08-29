package com.DBArena.services.catalog.service;

import com.DBArena.common.core.error.NotFoundException;

public class ProblemNotFoundException extends NotFoundException {

    public ProblemNotFoundException(String slug) {
        super("Problem", slug);
    }
}
