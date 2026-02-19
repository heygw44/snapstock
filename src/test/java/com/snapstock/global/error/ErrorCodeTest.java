package com.snapstock.global.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void ErrorCode_코드_9개_존재() {
        // when
        ErrorCode[] codes = ErrorCode.values();

        // then
        assertThat(codes).hasSize(9);
    }

    @Test
    void ErrorCode_INVALID_INPUT_400_상태코드() {
        // when
        HttpStatus status = ErrorCode.INVALID_INPUT.getHttpStatus();

        // then
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ErrorCode_메시지_null_아님() {
        for (ErrorCode code : ErrorCode.values()) {
            // when
            String message = code.getMessage();

            // then
            assertThat(message).isNotNull();
        }
    }
}
