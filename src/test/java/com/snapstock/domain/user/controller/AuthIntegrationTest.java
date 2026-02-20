package com.snapstock.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.TestcontainersConfiguration;
import com.snapstock.domain.user.dto.LoginRequest;
import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.TokenReissueRequest;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.repository.UserRepository;
import com.snapstock.global.auth.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REISSUE_URL = "/api/v1/auth/reissue";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String PROTECTED_URL = "/api/v1/protected";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password1!";
    private static final String TEST_NICKNAME = "테스터";
    private static final long EXPIRED_TOKEN_TTL_MS = 1L;
    private static final long EXPIRED_TOKEN_WAIT_MS = 50L;
    private static final long JWT_SECOND_BOUNDARY_WAIT_MS = 1100L;
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String EXPECTED_MAX_AGE = "Max-Age=1209600";
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

    @Value("${jwt.secret}")
    private String jwtSecret;

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

    private String extractRefreshToken(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data").path("refreshToken").asText();
    }

    private String extractSetCookieHeader(MvcResult result) {
        return result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    private String createExpiredToken() throws InterruptedException {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                jwtSecret, EXPIRED_TOKEN_TTL_MS, EXPIRED_TOKEN_TTL_MS);
        String token = expiredProvider.createAccessToken(1L, Role.USER);
        Thread.sleep(EXPIRED_TOKEN_WAIT_MS);
        return token;
    }

    private void assertRefreshCookieAttributes(String setCookie) {
        assertThat(setCookie).contains(REFRESH_COOKIE_NAME + "=");
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_HTTP_ONLY);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_SECURE);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_SAME_SITE);
        assertThat(setCookie).containsIgnoringCase(EXPECTED_COOKIE_PATH);
    }

    // --- Test Groups ---

    @Nested
    class 전체인증플로우 {

        @Test
        void 가입_로그인_인증API호출_토큰재발급_로그아웃_블랙리스트확인() throws Exception {
            // 1. 회원가입
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            // 2. 로그인
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = extractAccessToken(loginResult);
            String refreshToken = extractRefreshToken(loginResult);

            // 3. 인증 API 호출 — 인증 통과, 보호 엔드포인트 200 응답
            mockMvc.perform(get(PROTECTED_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // 4. 토큰 재발급
            TokenReissueRequest reissueRequest = new TokenReissueRequest(refreshToken);
            MvcResult reissueResult = mockMvc.perform(post(REISSUE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reissueRequest)))
                    .andExpect(status().isOk())
                    .andReturn();
            String newAccessToken = extractAccessToken(reissueResult);

            // 5. 로그아웃
            mockMvc.perform(post(LOGOUT_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                    .andExpect(status().isNoContent());

            // 6. 블랙리스트 확인 — 로그아웃된 토큰으로 접근 시 401
            mockMvc.perform(get(PROTECTED_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class 토큰거부 {

        @Test
        void 만료된AccessToken_인증API호출_401응답() throws Exception {
            // given
            String expiredToken = createExpiredToken();

            // when
            var result = mockMvc.perform(get(PROTECTED_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken));

            // then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        void 블랙리스트AccessToken_인증API호출_401응답() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = extractAccessToken(loginResult);

            mockMvc.perform(post(LOGOUT_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // when
            var result = mockMvc.perform(get(PROTECTED_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

            // then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        void 위변조된AccessToken_인증API호출_401응답() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = extractAccessToken(loginResult);
            String tamperedToken = accessToken + "tampered";

            // when
            var result = mockMvc.perform(get(PROTECTED_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken));

            // then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        void 잘못된형식토큰_인증API호출_401응답() throws Exception {
            // when
            var result = mockMvc.perform(get(PROTECTED_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt-token"));

            // then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class 쿠키기반재발급 {

        @Test
        void 쿠키로재발급_Body없이_쿠키Fallback_200응답() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String refreshToken = extractRefreshToken(loginResult);

            // when
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        void Body우선순위_Body와쿠키모두존재시_Body토큰사용() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String refreshToken = extractRefreshToken(loginResult);
            TokenReissueRequest request = new TokenReissueRequest(refreshToken);

            // when — body=유효토큰, cookie=쓰레기값
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .cookie(new Cookie(REFRESH_COOKIE_NAME, "garbage-value")));

            // then — body 토큰이 사용되어 200 성공
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists());
        }

        @Test
        void Body빈문자열_쿠키Fallback_200응답() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String refreshToken = extractRefreshToken(loginResult);
            TokenReissueRequest request = new TokenReissueRequest("");

            // when — body=빈문자열, cookie=유효토큰
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken)));

            // then — 쿠키 Fallback으로 200 성공
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists());
        }

        @Test
        void Body와쿠키모두없음_400응답() throws Exception {
            // when
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }
    }

    @Nested
    class 쿠키속성검증 {

        @Test
        void 로그인응답_쿠키속성_PRD정책일치() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            // when
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String setCookie = extractSetCookieHeader(loginResult);

            // then
            assertRefreshCookieAttributes(setCookie);
            assertThat(setCookie).contains(EXPECTED_MAX_AGE);
        }

        @Test
        void 재발급응답_쿠키속성_PRD정책일치() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String refreshToken = extractRefreshToken(loginResult);
            TokenReissueRequest request = new TokenReissueRequest(refreshToken);

            // when
            MvcResult reissueResult = mockMvc.perform(post(REISSUE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();
            String setCookie = extractSetCookieHeader(reissueResult);

            // then
            assertRefreshCookieAttributes(setCookie);
            assertThat(setCookie).contains(EXPECTED_MAX_AGE);
        }

        @Test
        void 로그아웃응답_쿠키만료속성_PRD정책일치() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String accessToken = extractAccessToken(loginResult);

            // when
            MvcResult logoutResult = mockMvc.perform(post(LOGOUT_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isNoContent())
                    .andReturn();
            String setCookie = extractSetCookieHeader(logoutResult);

            // then
            assertRefreshCookieAttributes(setCookie);
            assertThat(setCookie).contains(EXPIRED_MAX_AGE);
        }
    }

    @Nested
    class RefreshToken회전 {

        @Test
        void 재발급후_이전RefreshToken으로재발급시도_401응답() throws Exception {
            // given
            performSignUp(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            MvcResult loginResult = performLogin(TEST_EMAIL, TEST_PASSWORD);
            String originalRefreshToken = extractRefreshToken(loginResult);

            // JWT iat는 초 단위 — 동일 초에 생성된 토큰은 동일 문자열이므로 회전 검증을 위해 대기
            Thread.sleep(JWT_SECOND_BOUNDARY_WAIT_MS);

            TokenReissueRequest firstReissue = new TokenReissueRequest(originalRefreshToken);
            mockMvc.perform(post(REISSUE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstReissue)))
                    .andExpect(status().isOk());

            // when — 이전 Refresh Token으로 재시도
            TokenReissueRequest secondReissue = new TokenReissueRequest(originalRefreshToken);
            var result = mockMvc.perform(post(REISSUE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondReissue)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
        }
    }
}
