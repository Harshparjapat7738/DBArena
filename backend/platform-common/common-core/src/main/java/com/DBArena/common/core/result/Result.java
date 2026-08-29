package com.dbforge.common.core.result;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An explicit success/failure value, for domain flows where "throw an
 * exception" is too heavy-handed (e.g. a validation step whose failure is
 * an expected, handled branch rather than an exceptional one). Prefer
 * {@link com.dbforge.common.core.error.DomainException} subtypes for
 * errors that should propagate up to the web layer unhandled; reach for
 * {@code Result} when the caller is expected to branch on the outcome
 * right there.
 */
public sealed interface Result<T, E> {

    record Ok<T, E>(T value) implements Result<T, E> {
    }

    record Err<T, E>(E error) implements Result<T, E> {
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    default boolean isOk() {
        return this instanceof Ok<T, E>;
    }

    default boolean isErr() {
        return this instanceof Err<T, E>;
    }

    default <R> Result<R, E> map(Function<T, R> mapper) {
        return switch (this) {
            case Ok<T, E> ok -> Result.ok(mapper.apply(ok.value()));
            case Err<T, E> err -> Result.err(err.error());
        };
    }

    default <F> Result<T, F> mapErr(Function<E, F> mapper) {
        return switch (this) {
            case Ok<T, E> ok -> Result.ok(ok.value());
            case Err<T, E> err -> Result.err(mapper.apply(err.error()));
        };
    }

    default <R> Result<R, E> flatMap(Function<T, Result<R, E>> mapper) {
        return switch (this) {
            case Ok<T, E> ok -> mapper.apply(ok.value());
            case Err<T, E> err -> Result.err(err.error());
        };
    }

    default T orElse(T fallback) {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> ignored -> fallback;
        };
    }

    default T orElseGet(Function<E, T> fallback) {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> fallback.apply(err.error());
        };
    }

    default T getOrThrow(Function<E, ? extends RuntimeException> toException) {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> throw toException.apply(err.error());
        };
    }

    default T unwrap() {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> throw new NoSuchElementException("Result is Err: " + err.error());
        };
    }

    static <T, E> Result<T, E> of(Supplier<T> supplier, Function<RuntimeException, E> onError) {
        try {
            return Result.ok(supplier.get());
        } catch (RuntimeException e) {
            return Result.err(onError.apply(e));
        }
    }
}
