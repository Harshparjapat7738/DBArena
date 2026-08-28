package com.dbforge.common.core.pagination;

import java.util.Optional;

/**
 * A cursor-pagination request. Root CLAUDE.md conventions mandate cursor
 * pagination on every list endpoint - no offset/limit APIs.
 */
public record PageRequest(int limit, Optional<String> cursor) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 200;

    public PageRequest {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        cursor = cursor == null ? Optional.empty() : cursor.filter(c -> !c.isBlank());
    }

    public static PageRequest first() {
        return new PageRequest(DEFAULT_LIMIT, Optional.empty());
    }

    public static PageRequest first(int limit) {
        return new PageRequest(limit, Optional.empty());
    }

    public static PageRequest after(String cursor, int limit) {
        return new PageRequest(limit, Optional.ofNullable(cursor));
    }
}
