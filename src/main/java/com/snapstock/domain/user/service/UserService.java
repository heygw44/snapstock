package com.snapstock.domain.user.service;

import com.snapstock.domain.user.dto.SignUpRequest;
import com.snapstock.domain.user.dto.UserUpdateRequest;
import com.snapstock.domain.user.dto.UserResponse;
import com.snapstock.domain.user.entity.User;
import com.snapstock.domain.user.repository.UserRepository;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 20;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        validateEmailNotDuplicated(request.email());
        validateNicknameNotDuplicated(request.nickname());
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.nickname());
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional
    public void deleteMyAccount(Long userId) {
        User user = findActiveUser(userId);
        user.softDelete();
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        User user = findActiveUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
        validateAtLeastOneFieldProvided(request);
        User user = findActiveUser(userId);
        updateNicknameIfPresent(user, request.nickname());
        updatePasswordIfPresent(user, request.password());
        return UserResponse.from(user);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateNotDeleted(user);
        return user;
    }

    private void validateNotDeleted(User user) {
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }
    }

    private void validateAtLeastOneFieldProvided(UserUpdateRequest request) {
        if (!request.hasUpdatableField()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void updateNicknameIfPresent(User user, String nickname) {
        if (nickname == null) {
            return;
        }
        String normalized = normalizeNickname(nickname);
        validateNicknameNotDuplicatedByOthers(normalized, user.getId());
        user.updateNickname(normalized);
    }

    private String normalizeNickname(String nickname) {
        String trimmed = nickname.trim();
        if (trimmed.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validateNicknameLength(trimmed);
        return trimmed;
    }

    private void validateNicknameLength(String nickname) {
        if (nickname.length() < NICKNAME_MIN_LENGTH || nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void updatePasswordIfPresent(User user, String password) {
        if (password == null) {
            return;
        }
        user.updatePassword(passwordEncoder.encode(password));
    }

    private void validateEmailNotDuplicated(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateNicknameNotDuplicated(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateNicknameNotDuplicatedByOthers(String nickname, Long userId) {
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
