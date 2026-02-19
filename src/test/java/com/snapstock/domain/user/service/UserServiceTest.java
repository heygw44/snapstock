package com.snapstock.domain.user.service;

import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.UserResponse;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.domain.user.entity.User;
import com.snapstock.domain.user.repository.UserRepository;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("정상 가입 요청 시 UserResponse를 반환한다")
    void signUp_정상가입_UserResponse반환() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Password1!", "테스터");
        String encodedPassword = "$2a$10$encodedPassword";
        User savedUser = User.create("test@example.com", encodedPassword, "테스터");

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = userService.signUp(request);

        // then
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("테스터");
        assertThat(response.role()).isEqualTo(Role.USER.name());
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로 가입 시 DUPLICATE_EMAIL 예외가 발생한다")
    void signUp_이메일중복_예외발생() {
        // given
        SignUpRequest request = new SignUpRequest(
                "dup@example.com", "Password1!", "테스터");

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 가입 시 DUPLICATE_NICKNAME 예외가 발생한다")
    void signUp_닉네임중복_예외발생() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Password1!", "중복닉네임");

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("비밀번호가 BCrypt로 인코딩되어 저장된다")
    void signUp_비밀번호_BCrypt인코딩() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com", "Password1!", "테스터");
        String encodedPassword = "$2a$10$encodedPassword";

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.signUp(request);

        // then
        then(passwordEncoder).should().encode("Password1!");
        then(userRepository).should().save(any(User.class));
    }
}
