package com.snapstock.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("ErrorCode 6개가 존재한다")
    void ErrorCode_코드_6개_존재() {
        // when
        ErrorCode[] codes = ErrorCode.values();

        // then
        assertThat(codes).hasSize(6);
    }

    @Test
    @DisplayName("INVALID_INPUT은 400 상태코드를 갖는다")
    void ErrorCode_INVALID_INPUT_400_상태코드() {
        // when
        HttpStatus status = ErrorCode.INVALID_INPUT.getHttpStatus();

        // then
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("모든 ErrorCode는 null이 아닌 메시지를 갖는다")
    void ErrorCode_메시지_null_아님() {
        for (ErrorCode code : ErrorCode.values()) {
            // when
            String message = code.getMessage();

            // then
            assertThat(message).isNotNull();
        }
    }
}
