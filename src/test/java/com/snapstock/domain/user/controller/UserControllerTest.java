package com.snapstock.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.domain.user.dto.UserUpdateRequest;
import com.snapstock.domain.user.dto.UserResponse;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.service.UserService;
import com.snapstock.global.auth.ApiAccessDeniedHandler;
import com.snapstock.global.auth.ApiAuthenticationEntryPoint;
import com.snapstock.global.auth.JwtAuthenticationFilter;
import com.snapstock.global.auth.JwtTokenProvider;
import com.snapstock.global.auth.TokenRedisService;
import com.snapstock.global.config.SecurityConfig;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class UserControllerTest {

    private static final String MY_INFO_URL = "/api/v1/users/me";
    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private void mockAuthentication() {
        given(jwtTokenProvider.resolveToken(any())).willReturn(ACCESS_TOKEN);
        given(jwtTokenProvider.validateToken(ACCESS_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUserId(ACCESS_TOKEN)).willReturn(USER_ID);
        given(jwtTokenProvider.getRole(ACCESS_TOKEN)).willReturn(Role.USER);
        given(tokenRedisService.isBlacklisted(ACCESS_TOKEN)).willReturn(false);
    }

    @Nested
    class 내정보조회 {

        @Test
        void getMyInfo_정상요청_200응답() throws Exception {
            // given
            mockAuthentication();
            UserResponse response = new UserResponse(
                    USER_ID, "test@example.com", "테스터", "USER", LocalDateTime.now());
            given(userService.getMyInfo(USER_ID)).willReturn(response);

            // when
            var result = mockMvc.perform(get(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("테스터"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        void getMyInfo_미인증_401응답() throws Exception {
            // when
            var result = mockMvc.perform(get(MY_INFO_URL));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class 내정보수정 {

        @Test
        void updateMyInfo_닉네임변경_200응답() throws Exception {
            // given
            mockAuthentication();
            UserUpdateRequest request = new UserUpdateRequest("새닉네임", null);
            UserResponse response = new UserResponse(
                    USER_ID, "test@example.com", "새닉네임", "USER", LocalDateTime.now());
            given(userService.updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class)))
                    .willReturn(response);

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
        }

        @Test
        void updateMyInfo_비밀번호변경_200응답() throws Exception {
            // given
            mockAuthentication();
            UserUpdateRequest request = new UserUpdateRequest(null, "NewPass1!");
            UserResponse response = new UserResponse(
                    USER_ID, "test@example.com", "테스터", "USER", LocalDateTime.now());
            given(userService.updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class)))
                    .willReturn(response);

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.nickname").value("테스터"));
        }

        @Test
        void updateMyInfo_닉네임중복_409응답() throws Exception {
            // given
            mockAuthentication();
            UserUpdateRequest request = new UserUpdateRequest("중복닉네임", null);
            given(userService.updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class)))
                    .willThrow(new CustomException(ErrorCode.DUPLICATE_NICKNAME));

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_NICKNAME.getMessage()));
        }

        @Test
        void updateMyInfo_유효성검증실패_400응답() throws Exception {
            // given
            mockAuthentication();
            UserUpdateRequest request = new UserUpdateRequest("한", null);

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
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
        void updateMyInfo_빈바디_400응답() throws Exception {
            // given
            mockAuthentication();
            given(userService.updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class)))
                    .willThrow(new CustomException(ErrorCode.INVALID_INPUT));

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }

        @Test
        void updateMyInfo_닉네임공백만_400응답() throws Exception {
            // given
            mockAuthentication();
            given(userService.updateMyInfo(eq(USER_ID), any(UserUpdateRequest.class)))
                    .willThrow(new CustomException(ErrorCode.INVALID_INPUT));

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new UserUpdateRequest("   ", null))));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }

        @Test
        void updateMyInfo_미인증_401응답() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest("새닉네임", null);

            // when
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }
}
