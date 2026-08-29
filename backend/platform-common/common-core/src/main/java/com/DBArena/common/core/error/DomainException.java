package com.DBArena.common.core.error;

import java.util.Map;

/**
 * Base type for every error the domain layer raises deliberately (as
 * opposed to an unexpected bug). Web-facing modules translate these into
 * RFC 7807 {@code application/problem+json} responses - see root
 * CLAUDE.md "Conventions". This module does not depend on Spring or on
 * any HTTP type; {@link #status()} is a plain HTTP status code so the
 * mapping can happen at the edge without this class knowing about
 * {@code jakarta.servlet} or Spring MVC.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final int status;
    private final Map<String, Object> extensions;

    protected DomainException(String code, int status, String message, Map<String, Object> extensions) {
        super(message);
        this.code = requireNonBlank(code, "code");
        this.status = status;
        this.extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    protected DomainException(String code, int status, String message) {
        this(code, status, message, Map.of());
    }

    /** Stable machine-readable error code, e.g. {@code "problem.not_found"}. */
    public String code() {
        return code;
    }

    /** HTTP status the edge layer should map this to. */
    public int status() {
        return status;
    }

    /** Extra RFC 7807 "extension members" - never the exception's stack trace or internals. */
    public Map<String, Object> extensions() {
        return extensions;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
