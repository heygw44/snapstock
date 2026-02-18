package com.snapstock.global.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        mockMvc.perform(get("/test/custom-exception"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
    }

    @Test
    @DisplayName("Validation 실패 시 400과 fieldErrors 배열을 반환한다")
    void Validation_실패시_fieldErrors_응답() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("예상외 예외 발생 시 500과 INTERNAL_ERROR를 반환한다")
    void 예상외_예외_발생시_500_응답() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
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

        @GetMapping("/unexpected")
        void throwUnexpectedException() {
            throw new RuntimeException("unexpected error");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
