# User `/me` 리팩토링 가이드

## 1. 문서 목적

`/api/v1/users/me` 수정 API의 입력 정책을 실무형으로 고정하고, 구현/테스트/운영 기준을 한 문서로 통합한다.

- 대상 정책
1. `PATCH /api/v1/users/me` no-op 요청(`{}` 또는 유효 업데이트 필드 없음) 차단
2. 닉네임 정규화 + 정규화 후 검증

- PRD 정합성 기준
1. `PATCH /api/v1/users/me`: `200 OK` (정상 수정)
2. 유효성 실패: `400 INVALID_INPUT`
3. 중복 닉네임: `409 DUPLICATE_NICKNAME`
4. 인증 필요: `401 UNAUTHORIZED`

## 2. 현재 리스크 요약

1. `PATCH` 요청에서 변경 필드가 없어도 `200`이 반환되어 클라이언트 버그를 숨길 수 있다.
2. 닉네임이 공백-only여도 길이 조건만 만족하면 통과할 수 있다.
3. 정규화 정책 부재로 `"닉네임"`과 `"닉네임 "` 등 입력 일관성이 깨질 수 있다.

## 3. 목표 동작(결정 사항)

## 3.1 요청 필드 정책

요청 바디에서 `nickname`, `password` 중 하나 이상은 반드시 유효하게 제공되어야 한다.

| 케이스 | 결과 |
|---|---|
| `{}` | `400 INVALID_INPUT` |
| `{"nickname": null, "password": null}` | `400 INVALID_INPUT` |
| `{"nickname": "새닉네임"}` | `200 OK` |
| `{"password": "NewPass1!"}` | `200 OK` |
| `{"nickname": "   "}` | `400 INVALID_INPUT` |

## 3.2 닉네임 정규화 정책

닉네임은 저장/중복검사 전에 정규화한다.

1. 좌우 공백 제거(`trim`)
2. 정규화 결과가 blank면 `INVALID_INPUT`
3. 길이 2~20 검증
4. 본인 제외 중복 검사
5. 통과 시 엔티티 업데이트

참고: 본 문서 범위에서는 `trim`을 필수로 하고, Unicode 정규화(NFC/NFKC)는 선택 항목으로 분리한다.

## 4. 구현 가이드

## 4.1 DTO 계층

대상: `UserUpdateRequest`

핵심 원칙:
1. 필드 단건 검증은 DTO 어노테이션으로 처리
2. "최소 한 필드 필요" 같은 교차 필드 검증은 서비스에서 처리(또는 커스텀 클래스 레벨 Validator)

권장 보완:
1. `nickname`에 공백-only 차단 규칙 추가
2. `password`는 기존 정책(길이/패턴) 유지

예시(개념):
```java
public record UserUpdateRequest(String nickname, String password) {
    public boolean hasUpdatableField() {
        return nickname != null || password != null;
    }
}
```

## 4.2 서비스 계층

대상: `UserService#updateMyInfo`

처리 순서:
1. 활성 사용자 조회(존재/탈퇴 검증)
2. 요청이 no-op인지 검증
3. 닉네임 정규화/검증/중복검사/반영
4. 비밀번호 인코딩/반영
5. 응답 반환

권장 메서드 분리:
1. `validateAtLeastOneFieldProvided(...)`
2. `normalizeNickname(...)`
3. `updateNicknameIfPresent(...)`
4. `updatePasswordIfPresent(...)`

no-op 차단 예시(개념):
```java
private void validateAtLeastOneFieldProvided(UserUpdateRequest request) {
    boolean nicknameMissing = request.nickname() == null || request.nickname().isBlank();
    boolean passwordMissing = request.password() == null || request.password().isBlank();
    if (nicknameMissing && passwordMissing) {
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }
}
```

주의:
1. 닉네임은 정규화 후 중복검사해야 한다.
2. 비밀번호는 원문 저장 금지, 항상 `PasswordEncoder` 경유.
3. JPA dirty checking 기반이면 명시적 `save` 없이도 반영 가능하나, 트랜잭션 경계는 필수.

## 4.3 컨트롤러 계층

대상: `UserController#updateMyInfo`

원칙:
1. 컨트롤러는 인증 사용자 ID 전달 + 요청 바인딩 + 응답 매핑에 집중
2. 비즈니스 규칙(no-op, 정규화, 중복검증)은 서비스 책임

응답 계약:
1. 성공: `200 + ApiResponse.success(data)`
2. 실패: `GlobalExceptionHandler`를 통해 에러 envelope 반환

## 4.4 예외/에러코드 계층

현재 에러코드 활용:
1. no-op/공백 nickname 등 입력 문제: `INVALID_INPUT (400)`
2. 중복 닉네임: `DUPLICATE_NICKNAME (409)`
3. 탈퇴 유저: `DELETED_USER (401)`
4. 유저 없음: `USER_NOT_FOUND (404)`

권장:
1. no-op 전용 에러코드를 새로 만들지 않고 `INVALID_INPUT` 재사용
2. 메시지는 클라이언트 디버깅에 충분히 구체화

## 5. 테스트 리팩토링 가이드

## 5.1 서비스 테스트

추가/강화 케이스:
1. `{}` 또는 `(null, null)` 요청 시 `INVALID_INPUT`
2. nickname `"   "` 요청 시 `INVALID_INPUT`
3. nickname `"  새닉네임  "` 요청 시 trim 후 `"새닉네임"`으로 저장
4. 정규화 후 중복 검증이 동작하는지 확인
5. password-only 수정 시 인코딩 호출 검증

## 5.2 컨트롤러 테스트

추가/강화 케이스:
1. 빈 바디 `{}` -> `400 INVALID_INPUT`
2. nickname 공백-only -> `400 INVALID_INPUT`
3. 정상 nickname/password 수정 -> `200`
4. 인증 누락 -> `401`

## 5.3 통합 테스트(권장)

실제 요청 흐름으로 다음을 검증한다.

1. 로그인 후 `PATCH /users/me` `{}` -> `400`
2. `" nickname "` 요청 후 조회 시 trim 반영
3. 중복 닉네임 경합 시 최종적으로 `409` 보장(DB unique + 예외 매핑)

## 6. 단계별 적용 순서

1. `UserUpdateRequest` 보완(검증/헬퍼 추가)
2. `UserService`에 no-op 검증 + 닉네임 정규화 로직 추가
3. 단위 테스트(`UserServiceTest`) 먼저 보강
4. 컨트롤러 테스트(`UserControllerTest`) 보강
5. 필요 시 통합 테스트 추가
6. 전체 테스트 실행 후 머지

권장 검증 명령:
```bash
./gradlew test --tests com.snapstock.domain.user.service.UserServiceTest
./gradlew test --tests com.snapstock.domain.user.controller.UserControllerTest
```

## 7. 코드 리뷰 체크리스트

1. no-op 요청이 반드시 `400 INVALID_INPUT`으로 귀결되는가
2. nickname은 반드시 정규화 후 검증/저장되는가
3. 중복검사가 정규화 값 기준으로 수행되는가
4. 비밀번호가 인코딩 없이 저장되는 경로가 없는가
5. 기존 성공/실패 응답 envelope 규약이 유지되는가
6. 테스트가 정책을 고정하고 회귀를 막는가

## 8. 운영 고려사항

1. API 변경 공지: 기존 `{}` 허용 클라이언트가 있다면 배포 노트에 명시
2. 로그: validation 실패 원인은 남기되 비밀번호/토큰은 마스킹
3. 모니터링: `INVALID_INPUT` 비율 증가 시 클라이언트 릴리즈 결함 여부 확인

## 9. 확장 옵션

1. Unicode 정규화(NFC/NFKC) 도입
2. 닉네임 금칙어 정책
3. no-op 전용 에러코드 분리(필요 시)
4. 커스텀 클래스 레벨 Validator로 교차 필드 검증 이전
