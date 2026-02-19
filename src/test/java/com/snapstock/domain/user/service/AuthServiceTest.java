package com.snapstock.domain.user.service;

import com.snapstock.domain.user.dto.LoginRequest;
import com.snapstock.domain.user.dto.LoginResponse;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.entity.User;
import com.snapstock.domain.user.repository.UserRepository;
import com.snapstock.global.auth.JwtTokenProvider;
import com.snapstock.global.auth.TokenRedisService;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password1!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final long ACCESS_EXPIRATION_MS = 1_800_000L;
    private static final long REFRESH_EXPIRATION_MS = 1_209_600_000L;
    private static final long REMAINING_MS = 900_000L;
    private static final long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRedisService tokenRedisService;

    @InjectMocks
    private AuthService authService;

    @Nested
    class Login {

        @Test
        void login_정상요청_토큰반환() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");
            setUserId(user, USER_ID);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtTokenProvider.createAccessToken(USER_ID, Role.USER)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(ACCESS_EXPIRATION_MS / 1000);
        }

        @Test
        void login_존재하지않는이메일_LOGIN_FAILED() {
            // given
            LoginRequest request = new LoginRequest("unknown@example.com", TEST_PASSWORD);
            given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }

        @Test
        void login_비밀번호불일치_LOGIN_FAILED() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPass1!");
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("WrongPass1!", ENCODED_PASSWORD)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }

        @Test
        void login_탈퇴한유저_DELETED_USER() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");
            user.softDelete();

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DELETED_USER);
        }

        @Test
        void login_정상요청_RefreshToken_Redis저장() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");
            setUserId(user, USER_ID);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtTokenProvider.createAccessToken(USER_ID, Role.USER)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            authService.login(request);

            // then
            then(tokenRedisService).should()
                    .saveRefreshToken(USER_ID, REFRESH_TOKEN, REFRESH_EXPIRATION_MS);
        }
    }

    @Nested
    class Reissue {

        @Test
        void reissue_정상요청_새토큰쌍반환() {
            // given
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");
            setUserId(user, USER_ID);

            given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(tokenRedisService.getRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(USER_ID, Role.USER)).willReturn(NEW_ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(NEW_REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            LoginResponse response = authService.reissue(REFRESH_TOKEN);

            // then
            assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        }

        @Test
        void reissue_토큰누락_INVALID_INPUT() {
            // when & then
            assertThatThrownBy(() -> authService.reissue(null))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        void reissue_빈문자열_INVALID_INPUT() {
            // when & then
            assertThatThrownBy(() -> authService.reissue("   "))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        void reissue_유효하지않은토큰_INVALID_REFRESH_TOKEN() {
            // given
            given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.reissue("invalid-token"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        void reissue_Redis불일치_INVALID_REFRESH_TOKEN() {
            // given
            given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(tokenRedisService.getRefreshToken(USER_ID)).willReturn("different-token");

            // when & then
            assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        void reissue_탈퇴한유저_DELETED_USER() {
            // given
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");
            setUserId(user, USER_ID);
            user.softDelete();

            given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(tokenRedisService.getRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DELETED_USER);
        }

        @Test
        void reissue_정상요청_이전토큰삭제_Rotation() {
            // given
            User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, "테스터");
            setUserId(user, USER_ID);

            given(jwtTokenProvider.validateToken(REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(tokenRedisService.getRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(USER_ID, Role.USER)).willReturn(NEW_ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(NEW_REFRESH_TOKEN);
            given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(ACCESS_EXPIRATION_MS);
            given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(REFRESH_EXPIRATION_MS);

            // when
            authService.reissue(REFRESH_TOKEN);

            // then
            then(tokenRedisService).should().deleteRefreshToken(USER_ID);
            then(tokenRedisService).should()
                    .saveRefreshToken(USER_ID, NEW_REFRESH_TOKEN, REFRESH_EXPIRATION_MS);
        }
    }

    @Nested
    class Logout {

        @Test
        void logout_정상요청_블랙리스트등록() {
            // given
            given(jwtTokenProvider.validateToken(ACCESS_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getRemainingExpirationMs(ACCESS_TOKEN)).willReturn(REMAINING_MS);

            // when
            authService.logout(ACCESS_TOKEN, USER_ID);

            // then
            then(tokenRedisService).should().addToBlacklist(ACCESS_TOKEN, REMAINING_MS);
        }

        @Test
        void logout_정상요청_RefreshToken삭제() {
            // given
            given(jwtTokenProvider.validateToken(ACCESS_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getRemainingExpirationMs(ACCESS_TOKEN)).willReturn(REMAINING_MS);

            // when
            authService.logout(ACCESS_TOKEN, USER_ID);

            // then
            then(tokenRedisService).should().deleteRefreshToken(USER_ID);
        }

        @Test
        void logout_토큰null_UNAUTHORIZED() {
            // when & then
            assertThatThrownBy(() -> authService.logout(null, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        void logout_유효하지않은토큰_UNAUTHORIZED() {
            // given
            given(jwtTokenProvider.validateToken("invalid")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.logout("invalid", USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
