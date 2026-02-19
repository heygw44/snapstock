package com.snapstock.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.domain.user.dto.LoginRequest;
import com.snapstock.domain.user.dto.LoginResponse;
import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.TokenReissueRequest;
import com.snapstock.domain.user.dto.UserResponse;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.service.AuthService;
import com.snapstock.domain.user.service.UserService;
import com.snapstock.global.auth.ApiAccessDeniedHandler;
import com.snapstock.global.auth.ApiAuthenticationEntryPoint;
import com.snapstock.global.auth.JwtAuthenticationFilter;
import com.snapstock.global.auth.JwtTokenProvider;
import com.snapstock.global.auth.TokenRedisService;
import com.snapstock.global.config.SecurityConfig;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class AuthControllerTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REISSUE_URL = "/api/v1/auth/reissue";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String REFRESH_TOKEN = "valid-refresh-token";
    private static final long REFRESH_EXPIRATION_MS = 1_209_600_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    @Nested
    class SignUp {

        @Test
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

    @Nested
    class Login {

        @Test
        void login_정상요청_200응답() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "Password1!");
            LoginResponse response = LoginResponse.of(ACCESS_TOKEN, REFRESH_TOKEN, 1_800_000L);

            given(authService.login(any(LoginRequest.class))).willReturn(response);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            var result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.refreshToken").value(REFRESH_TOKEN))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(1800))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        void login_이메일형식오류_400응답() throws Exception {
            // given
            LoginRequest request = new LoginRequest("invalid-email", "Password1!");

            // when
            var result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }

        @Test
        void login_비밀번호빈값_400응답() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "");

            // when
            var result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')].value")
                            .value("[REDACTED]"));
        }

        @Test
        void login_인증실패_401응답() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "WrongPass1!");

            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new CustomException(ErrorCode.LOGIN_FAILED));

            // when
            var result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("LOGIN_FAILED"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.LOGIN_FAILED.getMessage()));
        }
    }

    @Nested
    class Reissue {

        @Test
        void reissue_정상요청_200응답() throws Exception {
            // given
            TokenReissueRequest request = new TokenReissueRequest(REFRESH_TOKEN);
            LoginResponse response = LoginResponse.of("new-access", "new-refresh", 1_800_000L);

            given(authService.reissue(REFRESH_TOKEN)).willReturn(response);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        void reissue_쿠키Fallback_200응답() throws Exception {
            // given
            LoginResponse response = LoginResponse.of("new-access", "new-refresh", 1_800_000L);

            given(authService.reissue(REFRESH_TOKEN)).willReturn(response);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(new Cookie("refreshToken", REFRESH_TOKEN)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access"));
        }

        @Test
        void reissue_토큰누락_400응답() throws Exception {
            // given
            given(authService.reissue(null))
                    .willThrow(new CustomException(ErrorCode.INVALID_INPUT));

            // when
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }
    }

    @Nested
    class Logout {

        @Test
        void logout_정상요청_204응답() throws Exception {
            // given
            given(jwtTokenProvider.resolveToken(any())).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.validateToken(ACCESS_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getUserId(ACCESS_TOKEN)).willReturn(1L);
            given(jwtTokenProvider.getRole(ACCESS_TOKEN)).willReturn(Role.USER);
            given(tokenRedisService.isBlacklisted(ACCESS_TOKEN)).willReturn(false);
            willDoNothing().given(authService).logout(ACCESS_TOKEN, 1L);

            // when
            var result = mockMvc.perform(post(LOGOUT_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN));

            // then
            result.andExpect(status().isNoContent())
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        void logout_미인증_401응답() throws Exception {
            // when
            var result = mockMvc.perform(post(LOGOUT_URL));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }
}
