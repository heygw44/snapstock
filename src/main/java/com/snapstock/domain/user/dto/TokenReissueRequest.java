package com.snapstock.domain.user.dto;

public record TokenReissueRequest(
        String refreshToken
) {
}
