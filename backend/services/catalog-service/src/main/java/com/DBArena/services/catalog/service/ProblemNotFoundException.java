package com.dbforge.services.catalog.service;

import com.dbforge.common.core.error.NotFoundException;

public class ProblemNotFoundException extends NotFoundException {

    public ProblemNotFoundException(String slug) {
        super("Problem", slug);
    }
}
