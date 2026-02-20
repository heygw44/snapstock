package com.snapstock.global.auth;

import org.springframework.http.ResponseCookie;

public final class RefreshCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";

    private static final String PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Lax";
    private static final long EXPIRE_IMMEDIATELY = 0L;
    private static final long MS_TO_SECONDS = 1000;

    private RefreshCookieProvider() {
    }

    public static ResponseCookie create(String token, long maxAgeMs) {
        return baseCookie(token)
                .maxAge(maxAgeMs / MS_TO_SECONDS)
                .build();
    }

    public static ResponseCookie expire() {
        return baseCookie("")
                .maxAge(EXPIRE_IMMEDIATELY)
                .build();
    }

    private static ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path(PATH);
    }
}
