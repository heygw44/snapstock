# SnapStock M1 리뷰 이슈 리팩토링 설계서

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0.1 |
| 작성일 | 2026-02-19 |
| 상태 | Completed |
| 범위 | M1 공통 모듈 + 보안 설정 + 운영 설정 |

---

## 1. 목적

M1 구현 코드 리뷰에서 확인된 5개 이슈를 실무 투입 가능한 품질로 정리하고, 구현자가 바로 반영 가능한 수준의 상세 설계를 제공한다.

### 1.1 해결 대상 이슈

1. Actuator 엔드포인트 과다 공개 (`/actuator/**` permitAll)
2. Security 필터 단계 401/403 응답이 API 표준 Envelope와 불일치 가능
3. Validation 에러 응답에서 민감값(rejectedValue) 노출 가능
4. CursorPageResponse 경계값(`size <= 0`) 미검증으로 런타임 예외
5. CursorPageResponse `subList` 뷰 반환으로 참조 공유 위험

### 1.2 비목표(Non-goals)

1. JWT 발급/검증 로직 구현(M2 범위)
2. 프론트엔드 UI 변경
3. 도메인 API 스펙 전면 개편

---

## 2. 아키텍처/설계 원칙

1. **계약 우선**: 모든 오류 응답은 `ApiResponse` Envelope를 유지한다.
2. **기본 보안 강화**: Public 노출은 최소 권한 원칙으로 제한한다.
3. **하위 호환 우선**: 성공 응답 포맷은 변경하지 않는다.
4. **Fail-fast**: 잘못된 파라미터는 조기에 명시적으로 거부한다.
5. **테스트 우선 보강**: 변경되는 동작은 단위/통합 테스트로 고정한다.

---

## 3. 상세 리팩토링 설계

## 3.1 Actuator 노출 축소

### 문제

- 현재 `SecurityConfig`가 `/actuator/**` 전체를 `permitAll` 처리한다.
- `application.yml`에서 `health, metrics, info`가 웹 노출되어 외부 정보 노출 위험이 있다.

### 목표 상태

1. 외부 공개 엔드포인트는 `/actuator/health`(및 하위 health path)만 허용한다.
2. 그 외 actuator 엔드포인트는 인증 필요(또는 비노출) 상태로 전환한다.
3. M1 완료 기준(`/actuator/health` 200)은 그대로 유지한다.

### 설계 변경

1. `SecurityConfig` 접근 정책 변경  
`/actuator/health`, `/actuator/health/**`만 `permitAll`  
`/actuator/**`는 `authenticated`

2. `application.yml` 노출 정책 조정  
`management.endpoints.web.exposure.include: health`로 축소  
(필요 시 `info`는 추후 운영 정책 승인 후 재오픈)

### 영향도

- 외부 모니터링이 `metrics/info`를 직접 호출하던 경우 접근 불가로 바뀜.
- M1 검증 시나리오는 영향 없음.

### 수용 기준

1. `/actuator/health`는 인증 없이 200
2. `/actuator/metrics`는 인증 없을 때 401

---

## 3.2 Security 401/403 Envelope 일원화

### 문제

- `@RestControllerAdvice`는 Controller 이후 예외만 처리 가능하다.
- 인증/인가 예외는 Security Filter 단계에서 발생해 기본 응답으로 빠질 수 있다.

### 목표 상태

1. 인증 실패(401), 인가 실패(403) 모두 `ApiResponse.error(...)` 형태로 응답한다.
2. 오류 코드 매핑은 `UNAUTHORIZED`, `FORBIDDEN`으로 고정한다.

### 설계 변경

1. 신규 컴포넌트 추가
  - `global/auth/ApiAuthenticationEntryPoint`
  - `global/auth/ApiAccessDeniedHandler`

2. `SecurityConfig`에 `exceptionHandling` 연결
  - `authenticationEntryPoint(...)`
  - `accessDeniedHandler(...)`

3. 공통 직렬화 정책
  - `Content-Type: application/json`
  - Body: `ApiResponse.error(ErrorCode.UNAUTHORIZED|FORBIDDEN)`

### 영향도

- 기존 기본 Spring Security 에러 JSON 형식에 의존하던 테스트/클라이언트는 수정 필요.
- 프로젝트 문서(PRD의 공통 응답 규약)와 정합성 상승.

### 수용 기준

1. 인증 없는 보호 리소스 요청 시 401 + Envelope
2. 권한 없는 리소스 요청 시 403 + Envelope

---

## 3.3 Validation 응답 민감값 마스킹

### 문제

- `MethodArgumentNotValidException` 처리 시 `rejectedValue`를 문자열 그대로 응답에 포함한다.
- 비밀번호/토큰 같은 민감 필드가 노출될 수 있다.

### 목표 상태

1. 민감 필드는 `value`를 노출하지 않는다.
2. 일반 필드는 디버깅 가능한 범위에서 값 노출을 허용한다.

### 설계 변경

1. `GlobalExceptionHandler` 내부 `private` 메서드로 마스킹 처리  
예: `private String sanitizeRejectedValue(String field, Object rejectedValue)`
2. 민감 필드 키워드(소문자 비교)  
`password`, `token`, `secret`, `authorization`, `credential`
3. 처리 규칙
  - 민감 필드: `"[REDACTED]"`
  - `null`: `null`
  - 일반 필드: `String.valueOf(value)` (길이 상한 100~200자로 절단 권장)

4. `GlobalExceptionHandler`에서 위 `private` 메서드를 통해 `FieldErrorResponse.value` 설정

### 영향도

- 검증 실패 응답의 `value` 필드가 일부 케이스에서 달라진다.
- 보안 감사 항목(민감정보 노출 금지) 충족.

### 수용 기준

1. `password` 검증 실패 시 `value`가 평문이 아니어야 함
2. 일반 필드(`name`) 검증 실패 시 value는 기존과 유사하게 확인 가능

---

## 3.4 CursorPageResponse 입력 검증(경계값)

### 문제

- `size <= 0`이면 내부 `subList`/`get` 호출에서 런타임 예외가 비의도적으로 발생한다.

### 목표 상태

1. 잘못된 입력은 `IllegalArgumentException`으로 명시적으로 거부한다.
2. `null` 인자도 동일하게 fail-fast 처리한다.

### 설계 변경

1. `CursorPageResponse.of(...)` 시작부 검증 추가
  - `Objects.requireNonNull(content, "...")`
  - `Objects.requireNonNull(idExtractor, "...")`
  - `if (size <= 0) throw new IllegalArgumentException("size must be greater than 0");`

2. `nextCursor`는 `idExtractor` 결과가 `null`이면 예외 처리(명시적 계약)

### 영향도

- 기존에는 우연히 통과/실패하던 잘못된 입력이 일관된 예외로 정리된다.

### 수용 기준

1. `size=0` 또는 `-1`에서 `IllegalArgumentException`
2. `content=null`, `idExtractor=null`에서 `NullPointerException` 또는 명시 예외

---

## 3.5 CursorPageResponse 불변성 강화(방어적 복사)

### 문제

- `subList`는 원본 리스트 뷰라서 원본 변형 시 응답 데이터가 흔들릴 수 있다.

### 목표 상태

1. 반환 `content`는 외부 변경에 영향받지 않는 불변 리스트다.

### 설계 변경

1. `List.copyOf(...)` 적용
  - hasNext=false: `List.copyOf(content)`
  - hasNext=true: `List.copyOf(content.subList(0, size))`

### 영향도

- 반환 리스트 수정 시 `UnsupportedOperationException` 발생(의도된 보호 동작)

### 수용 기준

1. 반환 content에 `add/remove` 시 예외 발생
2. 원본 리스트 변경이 반환 content에 영향 없음

---

## 4. 구현 순서(권장)

아래 순서는 리뷰 보완 의견 기준으로도 합리적인 순서로 확정한다
(`CursorPageResponse` 선반영 → Security → Validation).

1. `CursorPageResponse` 입력 검증 + 방어적 복사 반영
2. 관련 단위 테스트 보강
3. Security 401/403 핸들러 추가 및 `SecurityConfig` 연결
4. Actuator 노출 정책 축소 (`application.yml`, `SecurityConfig`)
5. Validation 마스킹 private 메서드 + `GlobalExceptionHandler` 반영
6. 통합 테스트/문서 검증

---

## 5. 테스트 설계

## 5.1 단위 테스트

1. `CursorPageResponseTest`
  - `size <= 0` 예외
  - `null` 인자 예외
  - 반환 리스트 불변성 검증

2. `GlobalExceptionHandlerTest`
  - 민감 필드(`password`) value 마스킹
  - 일반 필드 value 노출 유지

## 5.2 통합 테스트

1. `SecurityConfig` + MockMvc
  - `/actuator/health` 200 (무인증)
  - `/actuator/metrics` 401 + Envelope
  - 보호 API 무인증 401 + Envelope
  - 권한 부족 403 + Envelope

## 5.3 회귀 테스트

1. 기존 `ApiResponseTest`, `ErrorCodeTest`, `CustomExceptionTest` 전부 통과
2. `./gradlew clean test` 기준 전체 GREEN

---

## 6. 롤아웃/롤백 전략

### 롤아웃

1. 기능 브랜치에서 리팩토링 반영
2. 테스트 통과 후 머지
3. 배포 전 점검: actuator 접근 정책 + 오류 응답 샘플 캡처(OpenAPI/Postman)

### 롤백

1. 보안 설정 이슈 시 `SecurityConfig` 변경 커밋만 되돌림
2. Validation 마스킹 이슈 시 `GlobalExceptionHandler` 마스킹 로직 적용 커밋만 되돌림
3. Cursor 변경 이슈 시 `CursorPageResponse` 커밋만 되돌림

---

## 7. 리스크 및 대응

| 리스크 | 영향 | 대응 |
|---|---|---|
| `metrics/info` 차단으로 모니터링 공백 | 운영 가시성 저하 | 내부망/인증 기반 접근 정책 별도 정의 후 재개방 |
| 401/403 응답 포맷 변경 | 기존 소비자 파싱 오류 | OpenAPI/README 예시 동시 업데이트 |
| validation value 마스킹으로 디버깅 불편 | 장애 분석 난이도 증가 | 서버 로그에서만 상세값 관리(민감정보 제외) |

---

## 8. 완료 정의(Definition of Done)

1. 본 문서 5개 이슈가 코드로 모두 반영됨
2. 신규/수정 테스트가 모두 통과함
3. 401/403/Validation 에러 응답이 문서 규약과 일치함
4. `/actuator/health`만 공개 노출됨
5. CursorPageResponse가 경계값/불변성 요구를 충족함

---

## 9. 체크리스트

- [x] `SecurityConfig` actuator 정책 최소화
- [x] `ApiAuthenticationEntryPoint` 구현 (`global/auth/` 패키지)
- [x] `ApiAccessDeniedHandler` 구현 (`global/auth/` 패키지)
- [x] `GlobalExceptionHandler` 민감값 마스킹 반영
- [x] `CursorPageResponse` 입력 검증 추가
- [x] `CursorPageResponse` 방어적 복사 적용
- [x] 단위/통합 테스트 보강
- [x] `./gradlew clean test` 통과
