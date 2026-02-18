package com.snapstock.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("초기 ErrorCode 4개가 존재한다")
    void ErrorCode_초기코드_4개_존재() {
        assertThat(ErrorCode.values()).hasSize(4);
    }

    @Test
    @DisplayName("INVALID_INPUT은 400 상태코드를 갖는다")
    void ErrorCode_INVALID_INPUT_400_상태코드() {
        assertThat(ErrorCode.INVALID_INPUT.getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("모든 ErrorCode는 null이 아닌 메시지를 갖는다")
    void ErrorCode_메시지_null_아님() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.getMessage()).isNotNull();
        }
    }
}
