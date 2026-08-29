package com.DBArena.common.core.error;

import java.util.Map;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource, Object id) {
        super("resource.not_found", 404, resource + " not found: " + id,
                Map.of("resource", resource, "id", String.valueOf(id)));
    }

    public NotFoundException(String code, String message, Map<String, Object> extensions) {
        super(code, 404, message, extensions);
    }
}
