package com.snapstock.domain.user.dto;

import com.snapstock.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String nickname,
        String role,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
