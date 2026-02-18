package com.snapstock.global.common;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {

    private static final String ERR_NULL_CONTENT = "content must not be null";
    private static final String ERR_NULL_ID_EXTRACTOR = "idExtractor must not be null";
    private static final String ERR_INVALID_SIZE = "size must be greater than 0";

    public static <T> CursorPageResponse<T> of(List<T> content, int size,
                                                Function<T, Long> idExtractor) {
        validateParams(content, size, idExtractor);

        if (content.size() <= size) {
            return new CursorPageResponse<>(List.copyOf(content), null, false);
        }

        List<T> trimmed = List.copyOf(content.subList(0, size));
        Long nextCursor = idExtractor.apply(trimmed.get(trimmed.size() - 1));
        return new CursorPageResponse<>(trimmed, nextCursor, true);
    }

    private static <T> void validateParams(List<T> content, int size,
                                            Function<T, Long> idExtractor) {
        Objects.requireNonNull(content, ERR_NULL_CONTENT);
        Objects.requireNonNull(idExtractor, ERR_NULL_ID_EXTRACTOR);
        if (size <= 0) {
            throw new IllegalArgumentException(ERR_INVALID_SIZE);
        }
    }
}
