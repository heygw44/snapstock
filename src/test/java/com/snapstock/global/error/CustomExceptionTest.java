package com.snapstock.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionTest {

    @Test
    @DisplayName("CustomException 생성 시 ErrorCode를 보유한다")
    void CustomException_생성시_ErrorCode_보유() {
        // when
        CustomException exception = new CustomException(ErrorCode.UNAUTHORIZED);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("CustomException 메시지는 ErrorCode에서 전달된다")
    void CustomException_메시지_ErrorCode에서_전달() {
        // when
        CustomException exception = new CustomException(ErrorCode.FORBIDDEN);

        // then
        assertThat(exception.getMessage())
                .isEqualTo(ErrorCode.FORBIDDEN.getMessage());
    }
}
