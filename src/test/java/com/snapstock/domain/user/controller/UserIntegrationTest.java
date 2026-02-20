package com.snapstock.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.TestcontainersConfiguration;
import com.snapstock.domain.user.dto.LoginRequest;
import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.UserUpdateRequest;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String MY_INFO_URL = "/api/v1/users/me";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password1!";
    private static final String TEST_NICKNAME = "테스터";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String EXPIRED_MAX_AGE = "Max-Age=0";
    private static final String EXPECTED_COOKIE_HTTP_ONLY = "HttpOnly";
    private static final String EXPECTED_COOKIE_SECURE = "Secure";
    private static final String EXPECTED_COOKIE_SAME_SITE = "SameSite=Lax";
    private static final String EXPECTED_COOKIE_PATH = "Path=/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
        userRepository.deleteAll();
    }

    // --- Helper Methods ---

    private void performSignUp(String email, String password, String nickname) throws Exception {
        SignUpRequest request = new SignUpRequest(email, password, nickname);
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private MvcResult performLogin(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data").path("accessToken").asText();
    }

    private void performLogout(String accessToken) throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    private String signUpAndLogin(String email, String password, String nickname) throws Exception {
        performSignUp(email, password, nickname);
        MvcResult loginResult = performLogin(email, password);
        return extractAccessToken(loginResult);
    }

    private String extractSetCookieHeader(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(h -> h.startsWith(REFRESH_COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Refresh cookie not found"));
    }

    private void assertExpiredCookieAttributes(String setCookie) {
        assertThat(setCookie).contains(EXPIRED_MAX_AGE);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_HTTP_ONLY);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_SECURE);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_SAME_SITE);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_PATH);
    }

    // --- Test Groups ---

    @Nested
    class 전체프로필플로우 {

        @Test
        void 가입_로그인_조회_수정_탈퇴_탈퇴후로그인실패() throws Exception {
            // 1. 회원가입
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            // 2. 로그인
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = extractAccessToken(loginResult);

            // 3. GET /me — 200, 닉네임 확인
            mockMvc.perform(get(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME));

            // 4. PATCH /me — 닉네임 변경
            String newNickname = "새닉네임";
            UserUpdateRequest updateRequest = new UserUpdateRequest(newNickname, null);
            mockMvc.perform(patch(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value(newNickname));

            // 5. GET /me — 변경 확인
            mockMvc.perform(get(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value(newNickname));

            // 6. DELETE /me — 204
            mockMvc.perform(delete(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // 7. 로그인 시도 — DELETED_USER (401)
            LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("DELETED_USER"));
        }
    }

    @Nested
    class 내정보조회 {

        @Test
        void 정상조회_가입후로그인_200응답_필드일치() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = extractAccessToken(loginResult);

            // when
            var result = mockMvc.perform(get(MY_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME))
                    .andExpect(jsonPath("$.data.role").value(Role.USER.name()))
                    .andExpect(jsonPath("$.data.userId").isNumber())
                    .andExpect(jsonPath("$.data.createdAt").exists());
        }

        @Test
        void 탈퇴한유저_조회시도_401응답() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            mockMvc.perform(delete(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // when — 기존 토큰으로 조회 시도 (블랙리스트)
            var result = mockMvc.perform(get(MY_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class 내정보수정 {

        @Test
        void 닉네임변경_재조회시_변경닉네임반영() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            String newNickname = "새닉네임";
            UserUpdateRequest request = new UserUpdateRequest(newNickname, null);

            // when
            mockMvc.perform(patch(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // then — 재조회
            mockMvc.perform(get(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value(newNickname));
        }

        @Test
        void 비밀번호변경_새비밀번호로_재로그인성공() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            String newPassword = "NewPass1!";
            UserUpdateRequest request = new UserUpdateRequest(null, newPassword);

            mockMvc.perform(patch(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            performLogout(accessToken);

            // when — 새 비밀번호로 로그인
            MvcResult loginResult = performLogin(TEST_EMAIL, newPassword);

            // then
            String newAccessToken = extractAccessToken(loginResult);
            assertThat(newAccessToken).isNotBlank();
        }

        @Test
        void 비밀번호변경_기존비밀번호로_로그인실패() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            String newPassword = "NewPass1!";
            UserUpdateRequest request = new UserUpdateRequest(null, newPassword);

            mockMvc.perform(patch(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            performLogout(accessToken);

            // when — 기존 비밀번호로 로그인 시도
            LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            var result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("LOGIN_FAILED"));
        }

        @Test
        void 닉네임중복_다른유저와동일닉네임_409응답() throws Exception {
            // given — 유저A 가입
            String nicknameA = "유저에이";
            performSignUp("userA@example.com", TEST_PASSWORD, nicknameA);

            // 유저B 가입 후 로그인
            String accessTokenB = signUpAndLogin("userB@example.com", TEST_PASSWORD, "유저비이");

            // when — 유저B가 유저A 닉네임으로 변경 시도
            UserUpdateRequest request = new UserUpdateRequest(nicknameA, null);
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenB)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"));
        }

        @Test
        void 빈바디_수정필드없음_400응답() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            // when — 빈 바디 {} 전송 (실서비스 검증 경로 확인)
            var result = mockMvc.perform(patch(MY_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));

            // then — UserService.validateAtLeastOneFieldProvided → INVALID_INPUT
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }
    }

    @Nested
    class 회원탈퇴 {

        @Test
        void 탈퇴후_기존AccessToken_인증실패_401응답() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            mockMvc.perform(delete(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // when — 블랙리스트된 토큰으로 GET /me
            var result = mockMvc.perform(get(MY_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

            // then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        void 탈퇴후_로그인시도_DELETED_USER_401응답() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            mockMvc.perform(delete(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // when — 같은 이메일/비밀번호로 로그인 시도
            LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            var result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("DELETED_USER"));
        }

        @Test
        void 탈퇴응답_쿠키만료속성_PRD정책일치() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            // when
            MvcResult deleteResult = mockMvc.perform(delete(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andReturn();
            String setCookie = extractSetCookieHeader(deleteResult);

            // then
            assertExpiredCookieAttributes(setCookie);
        }

        @Test
        void 탈퇴후_재가입_동일이메일_409응답() throws Exception {
            // given
            String accessToken = signUpAndLogin(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            mockMvc.perform(delete(MY_INFO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // when — 동일 이메일로 재가입 시도
            SignUpRequest signUpRequest = new SignUpRequest(TEST_EMAIL, TEST_PASSWORD, "새닉네임");
            var result = mockMvc.perform(post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)));

            // then — 설계 결정: 탈퇴 이메일은 영구 예약 (재가입 불가)
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"));
        }
    }
}
