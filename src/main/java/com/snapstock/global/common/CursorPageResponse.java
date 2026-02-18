package com.snapstock.global.common;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {

    public static <T> CursorPageResponse<T> of(List<T> content, int size,
                                                Function<T, Long> idExtractor) {
        if (content.size() <= size) {
            return new CursorPageResponse<>(content, null, false);
        }

        List<T> trimmed = content.subList(0, size);
        Long nextCursor = idExtractor.apply(trimmed.get(trimmed.size() - 1));
        return new CursorPageResponse<>(trimmed, nextCursor, true);
    }
}
