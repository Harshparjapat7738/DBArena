package com.DBArena.services.catalog.service;

import com.DBArena.common.core.error.NotFoundException;

public class DatasetNotFoundException extends NotFoundException {

    public DatasetNotFoundException(String slug) {
        super("Dataset", slug);
    }
}
