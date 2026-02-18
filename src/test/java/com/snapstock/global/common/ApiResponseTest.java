package com.snapstock.global.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("success 응답은 SUCCESS 상태와 데이터를 갖는다")
    void success_응답_생성() {
        // given & when
        ApiResponse<String> response = ApiResponse.success("test");

        // then
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.data()).isEqualTo("test");
        assertThat(response.message()).isNull();
        assertThat(response.errorCode()).isNull();
        assertThat(response.fieldErrors()).isNull();
    }

    @Test
    @DisplayName("error 응답은 ERROR 상태와 에러 정보를 갖는다")
    void error_응답_생성() {
        // given & when
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNAUTHORIZED);

        // then
        assertThat(response.status()).isEqualTo("ERROR");
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
        assertThat(response.errorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("validationError 응답은 fieldErrors 배열을 갖는다")
    void validationError_응답_생성() {
        // given
        List<FieldErrorResponse> fieldErrors = List.of(
                new FieldErrorResponse("email", "invalid", "올바른 이메일 형식이 아닙니다.")
        );

        // when
        ApiResponse<Void> response = ApiResponse.validationError(fieldErrors);

        // then
        assertThat(response.status()).isEqualTo("ERROR");
        assertThat(response.errorCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.fieldErrors()).hasSize(1);
        assertThat(response.fieldErrors().get(0).field()).isEqualTo("email");
    }

    @Test
    @DisplayName("success 응답 JSON 직렬화 시 null 필드가 포함되지 않는다")
    void success_응답_JSON_null필드_미포함() throws Exception {
        // given
        ApiResponse<String> response = ApiResponse.success("data");

        // when
        String json = objectMapper.writeValueAsString(response);

        // then
        assertThat(json).contains("\"status\"");
        assertThat(json).contains("\"data\"");
        assertThat(json).doesNotContain("\"message\"");
        assertThat(json).doesNotContain("\"errorCode\"");
        assertThat(json).doesNotContain("\"fieldErrors\"");
    }
}
