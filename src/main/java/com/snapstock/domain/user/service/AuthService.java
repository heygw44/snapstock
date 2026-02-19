package com.snapstock.domain.user.service;

import com.snapstock.domain.user.dto.LoginRequest;
import com.snapstock.domain.user.dto.LoginResponse;
import com.snapstock.domain.user.entity.User;
import com.snapstock.domain.user.repository.UserRepository;
import com.snapstock.global.auth.JwtTokenProvider;
import com.snapstock.global.auth.TokenRedisService;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisService tokenRedisService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = findUserByEmail(request.email());
        validateNotDeleted(user);
        validatePassword(request.password(), user.getPassword());
        return issueTokens(user);
    }

    @Transactional
    public LoginResponse reissue(String refreshToken) {
        validateRefreshTokenPresent(refreshToken);
        validateRefreshTokenValid(refreshToken);
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        validateRefreshTokenMatches(userId, refreshToken);
        return rotateTokens(userId);
    }

    public void logout(String accessToken, Long userId) {
        validateAccessToken(accessToken);
        long remainingMs = jwtTokenProvider.getRemainingExpirationMs(accessToken);
        tokenRedisService.addToBlacklist(accessToken, remainingMs);
        tokenRedisService.deleteRefreshToken(userId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));
    }

    private void validateNotDeleted(User user) {
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        tokenRedisService.saveRefreshToken(
                user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirationMs());
        return LoginResponse.of(
                accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationMs());
    }

    private void validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateRefreshTokenPresent(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateRefreshTokenValid(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void validateRefreshTokenMatches(Long userId, String refreshToken) {
        String storedToken = tokenRedisService.getRefreshToken(userId);
        if (!refreshToken.equals(storedToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private LoginResponse rotateTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        validateNotDeleted(user);
        tokenRedisService.deleteRefreshToken(userId);
        return issueTokens(user);
    }
}
