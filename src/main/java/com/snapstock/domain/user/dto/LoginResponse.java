package com.snapstock.domain.user.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final long MS_TO_SECONDS = 1000;

    public static LoginResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE_BEARER, expiresInMs / MS_TO_SECONDS);
    }
}
