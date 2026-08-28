package com.dbforge.services.catalog.service;

import com.dbforge.common.core.error.ConflictException;

import java.util.Map;

public class DuplicateSlugException extends ConflictException {

    public DuplicateSlugException(String slug) {
        super("catalog.slug_already_exists", "A problem with slug '" + slug + "' already exists",
                Map.of("slug", slug));
    }
}
