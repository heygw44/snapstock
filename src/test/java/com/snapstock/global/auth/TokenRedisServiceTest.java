package com.snapstock.global.auth;

import com.snapstock.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TokenRedisServiceTest {

    private static final Long USER_ID = 1L;
    private static final String SAMPLE_TOKEN = "sample.jwt.token";
    private static final long EXPIRATION_MS = 60_000L;

    @Autowired
    private TokenRedisService tokenRedisService;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        redisConnectionFactory.getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Refresh Token을 저장하면 Redis에 저장된다")
    void saveRefreshToken_정상저장_Redis에저장됨() {
        // when
        tokenRedisService.saveRefreshToken(USER_ID, SAMPLE_TOKEN, EXPIRATION_MS);

        // then
        String stored = tokenRedisService.getRefreshToken(USER_ID);
        assertThat(stored).isEqualTo(SAMPLE_TOKEN);
    }

    @Test
    @DisplayName("존재하는 Refresh Token을 조회하면 토큰을 반환한다")
    void getRefreshToken_존재하는토큰_토큰반환() {
        // given
        tokenRedisService.saveRefreshToken(USER_ID, SAMPLE_TOKEN, EXPIRATION_MS);

        // when
        String result = tokenRedisService.getRefreshToken(USER_ID);

        // then
        assertThat(result).isEqualTo(SAMPLE_TOKEN);
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회하면 null을 반환한다")
    void getRefreshToken_존재하지않는userId_null반환() {
        // when
        String result = tokenRedisService.getRefreshToken(999L);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Refresh Token을 삭제하면 조회 시 null을 반환한다")
    void deleteRefreshToken_존재하는토큰_삭제됨() {
        // given
        tokenRedisService.saveRefreshToken(USER_ID, SAMPLE_TOKEN, EXPIRATION_MS);

        // when
        tokenRedisService.deleteRefreshToken(USER_ID);

        // then
        assertThat(tokenRedisService.getRefreshToken(USER_ID)).isNull();
    }

    @Test
    @DisplayName("블랙리스트에 등록하면 isBlacklisted가 true를 반환한다")
    void addToBlacklist_정상등록_블랙리스트존재() {
        // when
        tokenRedisService.addToBlacklist(SAMPLE_TOKEN, EXPIRATION_MS);

        // then
        assertThat(tokenRedisService.isBlacklisted(SAMPLE_TOKEN)).isTrue();
    }

    @Test
    @DisplayName("블랙리스트에 등록되지 않은 토큰은 false를 반환한다")
    void isBlacklisted_등록되지않은토큰_false반환() {
        // when & then
        assertThat(tokenRedisService.isBlacklisted("unknown-token")).isFalse();
    }
}
