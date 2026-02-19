package com.snapstock.global.config;

import org.springframework.context.annotation.Configuration;

// Decision: StringRedisTemplate은 Spring Boot 자동 구성 사용
// Alternatives: RedisTemplate<String, Object> + GenericJackson2JsonRedisSerializer
// Reason: 현재는 String key-value만 사용. M7-001에서 Cache DTO용 RedisTemplate 추가 예정
@Configuration
public class RedisConfig {
}
