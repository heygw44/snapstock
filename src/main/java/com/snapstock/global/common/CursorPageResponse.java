package com.snapstock.global.common;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {

    public static <T> CursorPageResponse<T> of(List<T> content, int size,
                                                Function<T, Long> idExtractor) {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(idExtractor, "idExtractor must not be null");
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }

        if (content.size() <= size) {
            return new CursorPageResponse<>(List.copyOf(content), null, false);
        }

        List<T> trimmed = List.copyOf(content.subList(0, size));
        Long nextCursor = idExtractor.apply(trimmed.get(trimmed.size() - 1));
        return new CursorPageResponse<>(trimmed, nextCursor, true);
    }
}
