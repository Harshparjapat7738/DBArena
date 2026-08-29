package com.dbforge.common.core.error;

import java.util.Map;

public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, 409, message);
    }

    public ConflictException(String code, String message, Map<String, Object> extensions) {
        super(code, 409, message, extensions);
    }
}
