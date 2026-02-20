package com.snapstock.domain.user.service;

import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.UserUpdateRequest;
import com.snapstock.domain.user.dto.UserResponse;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.entity.User;
import com.snapstock.domain.user.repository.UserRepository;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password1!";
    private static final String TEST_NICKNAME = "테스터";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User createTestUser() {
        User user = User.create(TEST_EMAIL, ENCODED_PASSWORD, TEST_NICKNAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private User createDeletedUser() {
        User user = createTestUser();
        user.softDelete();
        return user;
    }

    @Nested
    class 회원가입 {

        @Test
        void signUp_정상가입_UserResponse반환() {
            // given
            SignUpRequest request = new SignUpRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);
            User savedUser = createTestUser();

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByNickname(request.nickname())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            UserResponse response = userService.signUp(request);

            // then
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.nickname()).isEqualTo(TEST_NICKNAME);
            assertThat(response.role()).isEqualTo(Role.USER.name());
        }

        @Test
        void signUp_이메일중복_예외발생() {
            // given
            SignUpRequest request = new SignUpRequest("dup@example.com", TEST_PASSWORD, TEST_NICKNAME);

            given(userRepository.existsByEmail(request.email())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }

        @Test
        void signUp_닉네임중복_예외발생() {
            // given
            SignUpRequest request = new SignUpRequest(TEST_EMAIL, TEST_PASSWORD, "중복닉네임");

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByNickname(request.nickname())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        }

        @Test
        void signUp_비밀번호_BCrypt인코딩() {
            // given
            SignUpRequest request = new SignUpRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByNickname(request.nickname())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            userService.signUp(request);

            // then
            then(passwordEncoder).should().encode(TEST_PASSWORD);
            then(userRepository).should().save(any(User.class));
        }
    }

    @Nested
    class 내정보조회 {

        @Test
        void getMyInfo_정상조회_UserResponse반환() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            UserResponse response = userService.getMyInfo(USER_ID);

            // then
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.nickname()).isEqualTo(TEST_NICKNAME);
            assertThat(response.role()).isEqualTo(Role.USER.name());
        }

        @Test
        void getMyInfo_존재하지않는사용자_예외발생() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void getMyInfo_탈퇴한사용자_예외발생() {
            // given
            User deletedUser = createDeletedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(deletedUser));

            // when & then
            assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DELETED_USER);
        }
    }

    @Nested
    class 회원탈퇴 {

        @Test
        void deleteMyAccount_정상탈퇴_softDelete호출() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.deleteMyAccount(USER_ID);

            // then
            assertThat(user.isDeleted()).isTrue();
        }

        @Test
        void deleteMyAccount_존재하지않는사용자_예외발생() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteMyAccount(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void deleteMyAccount_이미탈퇴한사용자_예외발생() {
            // given
            User deletedUser = createDeletedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(deletedUser));

            // when & then
            assertThatThrownBy(() -> userService.deleteMyAccount(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DELETED_USER);
        }
    }

    @Nested
    class 내정보수정 {

        @Test
        void updateMyInfo_닉네임변경_성공() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("새닉네임", USER_ID)).willReturn(false);

            UserUpdateRequest request = new UserUpdateRequest("새닉네임", null);

            // when
            UserResponse response = userService.updateMyInfo(USER_ID, request);

            // then
            assertThat(response.nickname()).isEqualTo("새닉네임");
            then(passwordEncoder).should(never()).encode(anyString());
        }

        @Test
        void updateMyInfo_비밀번호변경_BCrypt인코딩() {
            // given
            User user = createTestUser();
            String newPassword = "NewPass1!";
            String newEncoded = "$2a$10$newEncodedPassword";

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(passwordEncoder.encode(newPassword)).willReturn(newEncoded);

            UserUpdateRequest request = new UserUpdateRequest(null, newPassword);

            // when
            userService.updateMyInfo(USER_ID, request);

            // then
            then(passwordEncoder).should().encode(newPassword);
        }

        @Test
        void updateMyInfo_닉네임과비밀번호동시변경_성공() {
            // given
            User user = createTestUser();
            String newPassword = "NewPass1!";
            String newEncoded = "$2a$10$newEncodedPassword";

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("새닉네임", USER_ID)).willReturn(false);
            given(passwordEncoder.encode(newPassword)).willReturn(newEncoded);

            UserUpdateRequest request = new UserUpdateRequest("새닉네임", newPassword);

            // when
            UserResponse response = userService.updateMyInfo(USER_ID, request);

            // then
            assertThat(response.nickname()).isEqualTo("새닉네임");
            then(passwordEncoder).should().encode(newPassword);
        }

        @Test
        void updateMyInfo_닉네임중복_예외발생() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("중복닉네임", USER_ID)).willReturn(true);

            UserUpdateRequest request = new UserUpdateRequest("중복닉네임", null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        }

        @Test
        void updateMyInfo_두필드모두null_예외발생() {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        void updateMyInfo_닉네임공백만_예외발생() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            UserUpdateRequest request = new UserUpdateRequest("   ", null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        void updateMyInfo_닉네임앞뒤공백_트림후저장() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("새닉네임", USER_ID)).willReturn(false);

            UserUpdateRequest request = new UserUpdateRequest("  새닉네임  ", null);

            // when
            UserResponse response = userService.updateMyInfo(USER_ID, request);

            // then
            assertThat(response.nickname()).isEqualTo("새닉네임");
            then(userRepository).should().existsByNicknameAndIdNot("새닉네임", USER_ID);
        }

        @Test
        void updateMyInfo_닉네임트림후길이부족_예외발생() {
            // given
            User user = createTestUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            UserUpdateRequest request = new UserUpdateRequest(" 한 ", null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        void updateMyInfo_존재하지않는사용자_예외발생() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            UserUpdateRequest request = new UserUpdateRequest("새닉네임", null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void updateMyInfo_탈퇴한사용자_예외발생() {
            // given
            User deletedUser = createDeletedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(deletedUser));

            UserUpdateRequest request = new UserUpdateRequest("새닉네임", null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DELETED_USER);
        }
    }
}
