package com.snapstock.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snapstock.global.error.ErrorCode;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,
        T data,
        String message,
        String errorCode,
        List<FieldErrorResponse> fieldErrors
) {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_ERROR = "ERROR";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(STATUS_SUCCESS, data, null, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode code) {
        return new ApiResponse<>(STATUS_ERROR, null, code.getMessage(), code.name(), null);
    }

    public static ApiResponse<Void> validationError(List<FieldErrorResponse> fieldErrors) {
        return new ApiResponse<>(
                STATUS_ERROR,
                null,
                ErrorCode.INVALID_INPUT.getMessage(),
                ErrorCode.INVALID_INPUT.name(),
                fieldErrors
        );
    }
}
