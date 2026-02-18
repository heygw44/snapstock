package com.snapstock.global.common;

public record FieldErrorResponse(
        String field,
        String value,
        String reason
) {
}
