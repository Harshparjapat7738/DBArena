package com.DBArena.common.core.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Opaque cursor encode/decode. Callers hand in the raw sort-key string
 * they want to resume from (e.g. {@code "<createdAt>|<id>"}); this class
 * only handles making that value transport-safe and non-obvious to
 * tamper with by hand. It is deliberately not a JSON codec: keep the raw
 * key format a private detail of the repository that produced it.
 */
public final class Cursors {

    private Cursors() {
    }

    public static String encode(String rawSortKey) {
        if (rawSortKey == null) {
            throw new IllegalArgumentException("rawSortKey must not be null");
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawSortKey.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new IllegalArgumentException("cursor must not be blank");
        }
        try {
            return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException(cursor, e);
        }
    }

    public static final class InvalidCursorException extends RuntimeException {
        public InvalidCursorException(String cursor, Throwable cause) {
            super("Malformed cursor", cause);
        }
    }
}
