package com.DBArena.common.core.error;

/** One field-level validation failure, field paths use dot notation (e.g. {@code "dataset.rows[3].value"}). */
public record FieldViolation(String field, String message) {

    public FieldViolation {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
