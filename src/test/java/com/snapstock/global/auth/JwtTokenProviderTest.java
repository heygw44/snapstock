package com.snapstock.global.auth;

import com.snapstock.domain.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "snapstock-test-secret-key-minimum-32-bytes-for-hmac-sha256";
    private static final long ACCESS_EXPIRATION_MS = 1_800_000L;
    private static final long REFRESH_EXPIRATION_MS = 1_209_600_000L;
    private static final Long USER_ID = 1L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("secret이 null이면 생성 시 예외가 발생한다")
    void constructor_null시크릿_예외발생() {
        assertThatThrownBy(() -> new JwtTokenProvider(null, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("secret이 32바이트 미만이면 생성 시 예외가 발생한다")
    void constructor_짧은시크릿_예외발생() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Access Token 생성 시 userId와 role claim이 포함된다")
    void createAccessToken_정상생성_userId와role포함() {
        // when
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);

        // then
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(USER_ID);
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Refresh Token 생성 시 userId만 포함되고 role은 포함되지 않는다")
    void createRefreshToken_정상생성_userId포함_role미포함() {
        // when
        String token = jwtTokenProvider.createRefreshToken(USER_ID);

        // then
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(USER_ID);
        assertThat(jwtTokenProvider.getRole(token)).isNull();
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 true를 반환한다")
    void validateToken_유효한토큰_true반환() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);

        // when & then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 false를 반환한다")
    void validateToken_만료된토큰_false반환() throws InterruptedException {
        // given
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 1L, 1L);
        String token = expiredProvider.createAccessToken(USER_ID, Role.USER);
        Thread.sleep(50);

        // when & then
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("변조된 토큰 검증 시 false를 반환한다")
    void validateToken_변조된토큰_false반환() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);
        String tamperedToken = token + "tampered";

        // when & then
        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("null 토큰 검증 시 false를 반환한다")
    void validateToken_null입력_false반환() {
        // when & then
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("유효한 토큰에서 userId를 추출한다")
    void getUserId_유효한토큰_userId반환() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);

        // when & then
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("유효한 토큰에서 role을 추출한다")
    void getRole_유효한토큰_role반환() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);

        // when & then
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("유효한 Bearer 헤더에서 토큰을 추출한다")
    void resolveToken_유효한BearerHeader_토큰반환() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        // when
        String resolved = jwtTokenProvider.resolveToken(request);

        // then
        assertThat(resolved).isEqualTo(token);
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 헤더에서 null을 반환한다")
    void resolveToken_BearerPrefix없음_null반환() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic some-token");

        // when & then
        assertThat(jwtTokenProvider.resolveToken(request)).isNull();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 null을 반환한다")
    void resolveToken_헤더없음_null반환() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when & then
        assertThat(jwtTokenProvider.resolveToken(request)).isNull();
    }

    @Test
    @DisplayName("유효한 토큰의 잔여 만료시간이 양수이다")
    void getRemainingExpirationMs_유효한토큰_양수반환() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID, Role.USER);

        // when
        long remaining = jwtTokenProvider.getRemainingExpirationMs(token);

        // then
        assertThat(remaining).isPositive();
    }
}
