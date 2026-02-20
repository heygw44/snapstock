package com.snapstock.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 전용 보호 엔드포인트.
 * SecurityConfig의 .anyRequest().authenticated() 동작을 검증하기 위한 목적.
 * src/test에만 존재하며 프로덕션 빌드에 포함되지 않는다.
 */
@RestController
public class ProtectedTestController {

    @GetMapping("/api/v1/protected")
    public String protectedEndpoint() {
        return "ok";
    }
}
