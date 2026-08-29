package com.dbforge.common.core.pagination;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** One page of a cursor-paginated list result. */
public record CursorPage<T>(List<T> items, Optional<String> nextCursor) {

    public CursorPage {
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }

    public static <T> CursorPage<T> lastPage(List<T> items) {
        return new CursorPage<>(items, Optional.empty());
    }

    public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
        return new CursorPage<>(items, Optional.ofNullable(nextCursor));
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }

    public <R> CursorPage<R> map(Function<T, R> mapper) {
        return new CursorPage<>(items.stream().map(mapper).toList(), nextCursor);
    }
}
