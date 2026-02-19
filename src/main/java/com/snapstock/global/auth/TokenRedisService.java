package com.snapstock.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String BLACKLIST_VALUE = "true";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long userId, String token, long expirationMs) {
        String key = REFRESH_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, token, expirationMs, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(Long userId) {
        return stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
    }

    public void deleteRefreshToken(Long userId) {
        stringRedisTemplate.delete(REFRESH_PREFIX + userId);
    }

    public void addToBlacklist(String accessToken, long remainingMs) {
        String key = BLACKLIST_PREFIX + hashToken(accessToken);
        stringRedisTemplate.opsForValue().set(key, BLACKLIST_VALUE, remainingMs, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + hashToken(accessToken);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
