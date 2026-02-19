package com.snapstock.global.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CustomException 발생 시 해당 ErrorCode의 상태코드와 에러 응답을 반환한다")
    void CustomException_발생시_비즈니스에러_응답() throws Exception {
        // when
        var result = mockMvc.perform(get("/test/custom-exception"));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
    }

    @Test
    @DisplayName("Validation 실패 시 400과 fieldErrors 배열을 반환한다")
    void Validation_실패시_fieldErrors_응답() throws Exception {
        // when
        var result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("예상외 예외 발생 시 500과 INTERNAL_ERROR를 반환한다")
    void 예상외_예외_발생시_500_응답() throws Exception {
        // when
        var result = mockMvc.perform(get("/test/unexpected"));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("민감 필드(password) 검증 실패 시 value가 마스킹된다")
    void Validation_민감필드_password_마스킹() throws Exception {
        // when
        var result = mockMvc.perform(post("/test/validation-sensitive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\",\"password\":\"ab\"}"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].value").value("[REDACTED]"));
    }

    @Test
    @DisplayName("일반 필드 검증 실패 시 value가 그대로 노출된다")
    void Validation_일반필드_value_노출() throws Exception {
        // when
        var result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].value").value(""));
    }

    @Test
    @DisplayName("이메일 unique 제약조건 위반 시 409와 DUPLICATE_EMAIL을 반환한다")
    void DataIntegrity_이메일_unique위반_409응답() throws Exception {
        // when
        var result = mockMvc.perform(get("/test/data-integrity-email"));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_EMAIL.getMessage()));
    }

    @Test
    @DisplayName("닉네임 unique 제약조건 위반 시 409와 DUPLICATE_NICKNAME을 반환한다")
    void DataIntegrity_닉네임_unique위반_409응답() throws Exception {
        // when
        var result = mockMvc.perform(get("/test/data-integrity-nickname"));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"));
    }

    @Test
    @DisplayName("알 수 없는 제약조건 위반 시 500과 INTERNAL_ERROR를 반환한다")
    void DataIntegrity_알수없는_제약조건위반_500응답() throws Exception {
        // when
        var result = mockMvc.perform(get("/test/data-integrity-unknown"));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/custom-exception")
        void throwCustomException() {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        @PostMapping("/validation")
        void validateRequest(@Valid @RequestBody TestRequest request) {
        }

        @PostMapping("/validation-sensitive")
        void validateSensitiveRequest(@Valid @RequestBody SensitiveRequest request) {
        }

        @GetMapping("/unexpected")
        void throwUnexpectedException() {
            throw new RuntimeException("unexpected error");
        }

        @GetMapping("/data-integrity-email")
        void throwDataIntegrityEmail() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLException("Duplicate entry 'a@b.com' for key 'users.ux_users_email'"));
        }

        @GetMapping("/data-integrity-nickname")
        void throwDataIntegrityNickname() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLException("Duplicate entry 'nick' for key 'users.ux_users_nickname'"));
        }

        @GetMapping("/data-integrity-unknown")
        void throwDataIntegrityUnknown() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLException("some unknown constraint violation"));
        }
    }

    record TestRequest(@NotBlank String name) {
    }

    record SensitiveRequest(
            @NotBlank String name,
            @Size(min = 8, max = 20) String password
    ) {
    }
}
