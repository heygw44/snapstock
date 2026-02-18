package com.snapstock.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snapstock.global.error.ErrorCode;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String status;
    private final T data;
    private final String message;
    private final String errorCode;
    private final List<FieldErrorResponse> fieldErrors;

    private ApiResponse(String status, T data, String message,
                        String errorCode, List<FieldErrorResponse> fieldErrors) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", data, null, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode code) {
        return new ApiResponse<>("ERROR", null, code.getMessage(), code.name(), null);
    }

    public static ApiResponse<Void> validationError(List<FieldErrorResponse> fieldErrors) {
        return new ApiResponse<>(
                "ERROR",
                null,
                ErrorCode.INVALID_INPUT.getMessage(),
                ErrorCode.INVALID_INPUT.name(),
                fieldErrors
        );
    }
}
