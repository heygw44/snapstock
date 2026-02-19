# SnapStock User 도메인 리팩토링 가이드 (Testcontainers 반영)

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.1.0 |
| 작성일 | 2026-02-19 |
| 기준 문서 | `docs/PRD.md` |
| 대상 범위 | `User` 엔티티/레포지토리/테스트 (`DataJpaTest + Testcontainers`) |

---

## 1. 목적

커밋 전 코드리뷰에서 확인된 이슈를, 현재 프로젝트의 **Testcontainers 기반 테스트 환경**을 전제로 안전하게 정리한다.

핵심 목표:
1. `UserRepositoryTest` 실패 원인 제거
2. PRD의 Soft Delete 정책과 쿼리 계약 정합성 확보
3. 불필요한 컨테이너 기동 제거로 테스트 실행 시간 단축
4. 계정 재사용 정책과 테스트 공통 설정 방식을 팀 표준으로 확정

---

## 2. 현재 이슈 요약

## 2.1 테스트 실패 (P0)

- 재현 명령:

```bash
./gradlew test --tests com.snapstock.domain.user.repository.UserRepositoryTest
```

- 실패 메시지 요약:
  - `Column 'created_at' cannot be null`
  - 위치: `userRepository.save(user)` 호출 시점

## 2.2 원인

1. `User`는 `BaseEntity`를 상속하며 `createdAt`, `updatedAt`가 `nullable = false`이다.
2. 값 주입은 `@EnableJpaAuditing` + `AuditingEntityListener`로 동작한다.
3. 하지만 `@DataJpaTest` 슬라이스 테스트에서는 auditing 설정이 누락될 수 있다.
4. 결과적으로 insert 시 audit 컬럼이 `null`로 저장되어 무결성 예외가 발생한다.

## 2.3 정책 정합성 리스크 (P1)

- PRD는 사용자 탈퇴를 Soft Delete로 정의한다.
- 현재 `UserRepository`는 `deletedAt` 조건이 없는 기본 조회/존재 확인만 제공한다.
- 이 상태로 서비스 로직을 구현하면 다음이 혼재될 수 있다.
  - 탈퇴 사용자 제외 조회
  - 탈퇴 사용자 포함 조회 (로그인 시 `DELETED_USER` 판단 필요)

즉, **의도별 조회 메서드 분리**가 필요하다.

## 2.4 테스트 효율 리스크 (P2)

- `UserRepositoryTest`는 JPA 검증만 필요하지만 Redis 컨테이너도 함께 기동하고 있다.
- 현재 `TestcontainersConfiguration`이 MySQL/Redis를 모두 등록하기 때문이며, 테스트 시간이 불필요하게 증가한다.

---

## 3. 리팩토링 원칙

1. 테스트는 항상 재현 가능해야 한다 (로컬/CI 동일).
2. Soft Delete 정책은 메서드 이름만 봐도 의도가 드러나야 한다.
3. JPA 슬라이스 테스트는 필요한 컨테이너만 띄운다.
4. PRD의 에러코드(`DELETED_USER`)를 구현 가능한 조회 계약으로 먼저 고정한다.

---

## 3.1 정책 확정안 (권장)

1. `email`: 탈퇴 후에도 재사용 비허용
2. `nickname`: 탈퇴 후 재사용 허용 (기본 즉시 허용, 필요 시 운영 쿨다운 추가)
3. JPA 슬라이스 테스트(`@DataJpaTest`): auditing을 개별 `@Import`가 아닌 공통 메타 애노테이션으로 활성화

---

## 4. 상세 리팩토링 단계

## 4.1 1단계: Auditing 공통화 (필수)

### 변경 목표

`@DataJpaTest`에서도 `createdAt`, `updatedAt`가 자동 입력되도록 보장한다.

### 권장 구현 (실무형)

1. `JpaAuditingTestConfig` 생성
2. `JpaRepositorySliceTest` 메타 애노테이션 생성
3. 모든 Repository 슬라이스 테스트는 메타 애노테이션으로 통일

`src/test/java/com/snapstock/support/JpaAuditingTestConfig.java`:

```java
@TestConfiguration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingTestConfig {
}
```

`src/test/java/com/snapstock/support/JpaRepositorySliceTest.java`:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MySqlTestcontainersConfiguration.class, JpaAuditingTestConfig.class})
public @interface JpaRepositorySliceTest {
}
```

Repository 테스트에서:

```java
@JpaRepositorySliceTest
class UserRepositoryTest {
    ...
}
```

---

## 4.2 2단계: Testcontainers 구성 분리 (권장)

### 변경 목표

JPA 테스트에서 Redis 컨테이너 기동 제거.

### 변경안

1. 기존 `TestcontainersConfiguration`을 용도별로 분리
  - `MySqlTestcontainersConfiguration`
  - `RedisTestcontainersConfiguration`
2. `UserRepositoryTest`는 MySQL 설정만 import

예시:

```java
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestcontainersConfiguration {
    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));
    }
}
```

```java
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestcontainersConfiguration {
    @Bean
    @ServiceConnection
    RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7-alpine"));
    }
}
```

---

## 4.3 3단계: Soft Delete 조회 계약 명시화 (필수)

### 변경 목표

`UserRepository`에서 "활성 사용자 전용 조회"와 "탈퇴 포함 조회"를 분리한다.

### 권장 메서드 계약

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // 탈퇴 포함 (로그인/DELETED_USER 판별용)
    boolean existsByEmail(String email);      // 탈퇴 포함 (email 재사용 비허용)

    Optional<User> findByEmailAndDeletedAtIsNull(String email); // 활성 사용자 조회
    boolean existsByNicknameAndDeletedAtIsNull(String nickname);// 활성 중복 체크
}
```

### 서비스 레이어 사용 가이드

1. 로그인:
  - `findByEmail(email)` 호출
  - 조회됨 + `isDeleted() == true` => `DELETED_USER`
2. 일반 사용자 조회:
  - `findByEmailAndDeletedAtIsNull`
3. 회원가입 중복체크:
  - `email`: `existsByEmail` (탈퇴 포함 체크)
  - `nickname`: `existsByNicknameAndDeletedAtIsNull` (활성 사용자만 체크)

### DB 제약 가이드 (MySQL 8.4)

1. `email` 유니크 제약은 유지한다 (`UNIQUE(email)`).
2. `nickname`은 "활성 사용자만 유니크"가 되도록 조정한다.
3. MySQL partial index 부재를 고려해 generated column + unique index를 사용한다.
4. 기존 nickname 유니크 인덱스명은 환경마다 다를 수 있으므로 `SHOW INDEX FROM users;`로 실제 이름을 확인 후 제거한다.

```sql
-- 1) 실제 nickname unique 인덱스명 확인
SHOW INDEX FROM users;

-- 2) 실제 인덱스명으로 교체 후 제거 (예시: UK_users_nickname)
ALTER TABLE users DROP INDEX {nickname_unique_index_name};

-- 3) 활성 사용자 전용 유니크 인덱스 추가
ALTER TABLE users
  ADD COLUMN active_nickname VARCHAR(50)
  GENERATED ALWAYS AS (
    CASE WHEN deleted_at IS NULL THEN nickname ELSE NULL END
  ) STORED,
  ADD UNIQUE INDEX ux_users_active_nickname (active_nickname);
```

---

## 4.4 4단계: 테스트 케이스 보강 (필수)

`UserRepositoryTest`에 아래 케이스를 추가한다.

1. `save` 시 `createdAt`, `updatedAt`가 null 아님
2. `findByEmailAndDeletedAtIsNull`:
  - 활성 사용자 조회 성공
  - soft delete 후 조회 실패
3. `findByEmail`:
  - soft delete 후에도 조회됨
4. `existsByEmail`:
  - soft delete 전 true
  - soft delete 후에도 true (재사용 비허용 정책)
5. `existsByNicknameAndDeletedAtIsNull`:
  - soft delete 전 true
  - soft delete 후 false (재사용 허용 정책)

보조 단위 테스트:
1. `UserTest`에 `softDelete` 호출 후 `isDeleted()==true`는 이미 존재, 유지

---

## 5. 권장 작업 순서

1. `JpaAuditingTestConfig` + `JpaRepositorySliceTest` 도입
2. `UserRepositoryTest` 그린 확인
3. 컨테이너 설정 분리 (MySQL/Redis)
4. `UserRepository` 메서드 계약 분리
5. DB 제약(email/nickname) 정책 반영
6. Soft Delete 관련 테스트 추가
7. 전체 테스트 실행

---

## 6. 검증 커맨드

```bash
./gradlew test --tests com.snapstock.domain.user.entity.UserTest
./gradlew test --tests com.snapstock.domain.user.entity.RoleTest
./gradlew test --tests com.snapstock.domain.user.repository.UserRepositoryTest
./gradlew test
```

---

## 7. 수용 기준 (Definition of Done)

1. `UserRepositoryTest`가 Testcontainers(MySQL) 환경에서 안정적으로 통과한다.
2. audit 컬럼 null 무결성 예외가 재발하지 않는다.
3. repository 메서드 이름만으로 Soft Delete 포함/제외 의도가 구분된다.
4. `email`은 탈퇴 후에도 중복 불가, `nickname`은 활성 사용자 기준 중복 불가가 테스트로 보장된다.
5. Soft Delete 시나리오 테스트가 추가되어 회귀를 방지한다.
6. JPA 테스트에서 Redis 컨테이너가 불필요하게 기동하지 않는다.

---

## 8. 롤백 포인트

1. Auditing 공통화(메타 애노테이션) 커밋
2. Testcontainers 설정 분리 커밋
3. Repository 계약 + DB 제약 변경 커밋
4. 테스트 보강 커밋

문제 발생 시 단계별로 롤백 가능하도록 커밋을 분리한다.
