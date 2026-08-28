package com.dbforge.engine.spi.model;

/** An engine-reported failure (syntax error, constraint violation, timeout) translated out of the driver's native exception type. */
public record ExecutionError(String code, String message) {

    public ExecutionError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
