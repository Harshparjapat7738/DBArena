package com.dbforge.common.core.id;

import java.io.Serializable;

/**
 * A phantom-typed identifier: {@code TypedId<Problem>} and {@code TypedId<User>}
 * are both backed by a plain string but are not assignable to one another,
 * so a service can't accidentally pass a user id where a problem id is
 * expected. {@code T} never appears at runtime - it exists purely for the
 * compiler.
 *
 * <p>Downstream modules use this directly as their id type, e.g.
 * {@code record Problem(TypedId<Problem> id, ...)}. Do not unwrap the
 * value to compare across entity types.
 */
public record TypedId<T>(String value) implements Serializable, Comparable<TypedId<T>> {

    public TypedId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("id value must not be blank");
        }
    }

    public static <T> TypedId<T> of(String value) {
        return new TypedId<>(value);
    }

    @Override
    public int compareTo(TypedId<T> other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
