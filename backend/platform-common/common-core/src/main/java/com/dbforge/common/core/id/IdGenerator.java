package com.dbforge.common.core.id;

/**
 * Generates new, lexicographically-sortable, globally-unique id values.
 * One instance is shared across a service; implementations must be
 * thread-safe.
 */
public interface IdGenerator {

    /** A new opaque id value, ready to be wrapped in a {@link TypedId}. */
    String next();

    default <T> TypedId<T> nextTyped() {
        return TypedId.of(next());
    }
}
