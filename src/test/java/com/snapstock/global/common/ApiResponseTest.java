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
        ApiResponse<String> response = ApiResponse.success("test");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getData()).isEqualTo("test");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("error 응답은 ERROR 상태와 에러 정보를 갖는다")
    void error_응답_생성() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNAUTHORIZED);

        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("validationError 응답은 fieldErrors 배열을 갖는다")
    void validationError_응답_생성() {
        List<FieldErrorResponse> fieldErrors = List.of(
                new FieldErrorResponse("email", "invalid", "올바른 이메일 형식이 아닙니다.")
        );

        ApiResponse<Void> response = ApiResponse.validationError(fieldErrors);

        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.getErrorCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getFieldErrors()).hasSize(1);
        assertThat(response.getFieldErrors().get(0).field()).isEqualTo("email");
    }

    @Test
    @DisplayName("success 응답 JSON 직렬화 시 null 필드가 포함되지 않는다")
    void success_응답_JSON_null필드_미포함() throws Exception {
        ApiResponse<String> response = ApiResponse.success("data");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\"");
        assertThat(json).contains("\"data\"");
        assertThat(json).doesNotContain("\"message\"");
        assertThat(json).doesNotContain("\"errorCode\"");
        assertThat(json).doesNotContain("\"fieldErrors\"");
    }
}
