package com.snapstock.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.UserResponse;
import com.snapstock.domain.user.service.UserService;
import com.snapstock.global.auth.ApiAccessDeniedHandler;
import com.snapstock.global.auth.ApiAuthenticationEntryPoint;
import com.snapstock.global.auth.JwtAuthenticationFilter;
import com.snapstock.global.auth.JwtTokenProvider;
import com.snapstock.global.auth.TokenRedisService;
import com.snapstock.global.config.SecurityConfig;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ApiAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private ApiAccessDeniedHandler accessDeniedHandler;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    @Test
    @DisplayName("정상 회원가입 요청 시 201 응답과 유저 정보를 반환한다")
    void signUp_정상요청_201응답() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Password1!", "테스터");
        UserResponse response = new UserResponse(
                1L, "test@example.com", "테스터", "USER", LocalDateTime.now());

        given(userService.signUp(any(SignUpRequest.class))).willReturn(response);

        // when
        var result = mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("테스터"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 응답과 fieldErrors를 반환한다")
    void signUp_이메일형식오류_400응답() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "invalid-email", "Password1!", "테스터");

        // when
        var result = mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400 응답과 fieldErrors를 반환한다")
    void signUp_비밀번호8자미만_400응답() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Pass1!", "테스터");

        // when
        var result = mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')].value")
                        .value("[REDACTED]"));
    }

    @Test
    @DisplayName("닉네임이 빈값이면 400 응답과 fieldErrors를 반환한다")
    void signUp_닉네임빈값_400응답() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Password1!", "");

        // when
        var result = mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'nickname')]").exists());
    }

    @Test
    @DisplayName("이메일 중복 시 409 응답과 DUPLICATE_EMAIL 에러코드를 반환한다")
    void signUp_이메일중복_409응답() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "dup@example.com", "Password1!", "테스터");

        given(userService.signUp(any(SignUpRequest.class)))
                .willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL));

        // when
        var result = mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_EMAIL.getMessage()));
    }

    @Test
    @DisplayName("닉네임 중복 시 409 응답과 DUPLICATE_NICKNAME 에러코드를 반환한다")
    void signUp_닉네임중복_409응답() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Password1!", "중복닉네임");

        given(userService.signUp(any(SignUpRequest.class)))
                .willThrow(new CustomException(ErrorCode.DUPLICATE_NICKNAME));

        // when
        var result = mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_NICKNAME.getMessage()));
    }
}
