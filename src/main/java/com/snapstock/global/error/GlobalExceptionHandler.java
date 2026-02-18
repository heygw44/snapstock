package com.snapstock.global.error;

import com.snapstock.global.common.ApiResponse;
import com.snapstock.global.common.FieldErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REDACTED = "[REDACTED]";
    private static final String ELLIPSIS = "...";
    private static final int VALUE_MAX_LENGTH = 200;
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "token", "secret", "authorization", "credential"
    );

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("Business exception: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        log.warn("Validation exception: {}", e.getMessage());
        List<FieldErrorResponse> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        sanitizeValue(error),
                        error.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.validationError(fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception: ", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    private String sanitizeValue(FieldError error) {
        if (error.getRejectedValue() == null) {
            return null;
        }
        if (isSensitiveField(error.getField())) {
            return REDACTED;
        }
        String value = String.valueOf(error.getRejectedValue());
        if (value.length() > VALUE_MAX_LENGTH) {
            return value.substring(0, VALUE_MAX_LENGTH) + ELLIPSIS;
        }
        return value;
    }

    private boolean isSensitiveField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return SENSITIVE_FIELDS.stream().anyMatch(lower::contains);
    }
}
