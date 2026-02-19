# SnapStock Milestones & Issue Tracker

> 9주 개발 로드맵 — 코딩 에이전트(Claude Code)가 이슈 단위로 실행 가능한 수준으로 세분화

| 표기 | 의미 |
|---|---|
| `M1-001` | 마일스톤 1의 1번 이슈 |
| ⚡ | 프로젝트 핵심 (동시성 제어) |
| 🔒 | 선행 이슈 의존 (blocked by) |
| `@agent` | 권장 에이전트 |
| ✅ | 완료 / ☐ 미완료 |

---

## M1: 프로젝트 초기 설정 (Week 1)

> **목표**: 빈 프로젝트에서 `./gradlew bootRun` 성공까지. 인프라 + 글로벌 모듈 완성.
> **완료 기준**: Docker Compose 실행 → Spring Boot 기동 → `/actuator/health` 200 OK

### 인프라 기초

- [x] **M1-001** — Spring Boot 프로젝트 초기화 `@planner`
  - Spring Initializr: Java 21, Gradle Groovy, Spring Boot 3.5.10
  - Dependencies: Web, JPA, Security, Validation, Redis, Actuator, Lombok, MySQL Driver
  - `.gitignore` (Gradle, IDE, .env)
  - 빈 `SnapStockApplication.java` 실행 확인

- [x] **M1-002** — Docker Compose 환경 구성
  - `docker-compose.yml`: MySQL 8.4 + Redis 7 alpine
  - MySQL: root 비밀번호 환경변수, `snapstock` DB 자동 생성
  - Redis: 6379 포트 매핑
  - health check 설정 (MySQL: `mysqladmin ping`, Redis: `redis-cli ping`)
  - `docker-compose up -d` → 컨테이너 정상 기동 확인

- [x] **M1-003** — application.yml 프로파일 분리
  - `application.yml`: 공통 설정 (server.port, actuator)
  - `application-local.yml`: Docker MySQL/Redis 접속 정보, ddl-auto: update
  - `application-test.yml`: Testcontainers용 (datasource 없음, ddl-auto: create-drop)
  - `spring.jpa.open-in-view: false` 설정
  - HikariCP: `maximum-pool-size: 10`, `leak-detection-threshold: 30000`

- [x] **M1-004** — Gradle 의존성 정리 + 빌드 확인
  - `build.gradle` 의존성 정리 (버전 관리: Spring Boot BOM 활용)
  - Testcontainers BOM 추가
  - 버전 고정 정책 적용: `latest` 태그/문구 금지, MySQL/Redis/Testcontainers 이미지 태그 고정
  - `./gradlew build` 성공 확인

### 글로벌 모듈 — 공통 인프라

- [x] **M1-005** — BaseEntity 생성 `@planner`
  - `global/common/BaseEntity.java`
  - `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`
  - `createdAt` (`@CreatedDate`, `updatable = false`), `updatedAt` (`@LastModifiedDate`)
  - `@EnableJpaAuditing` 설정 클래스

- [x] **M1-006** — ApiResponse 공통 응답 객체
  - `global/common/ApiResponse.java`
  - 필드: `status` (SUCCESS/ERROR), `data`, `message`, `errorCode`
  - `static success(T data)`, `static error(ErrorCode)`, `static validationError(List<FieldErrorResponse>)`
  - `FieldErrorResponse` record: `field`, `value`, `reason`

- [x] **M1-007** — ErrorCode enum + CustomException
  - `global/error/ErrorCode.java`: `HttpStatus` + `message` 매핑
  - 초기 에러코드: `INVALID_INPUT(400)`, `UNAUTHORIZED(401)`, `FORBIDDEN(403)`, `INTERNAL_ERROR(500)`
  - `global/error/CustomException.java`: `RuntimeException` 상속, `ErrorCode` 필드

- [x] **M1-008** — GlobalExceptionHandler
  - `global/error/GlobalExceptionHandler.java`
  - `@RestControllerAdvice`
  - `CustomException` → 비즈니스 예외 처리 (WARN 로그)
  - `MethodArgumentNotValidException` → Validation 에러 (fieldErrors 배열)
  - `Exception` → 예상 외 오류 (ERROR 로그, 500 응답)

- [x] **M1-009** — CursorPageResponse 공통 페이징 객체
  - `global/common/CursorPageResponse.java`
  - 필드: `List<T> content`, `Long nextCursor`, `boolean hasNext`
  - `static of(List<T> content, int size, Function<T, Long> idExtractor)`
  - size + 1 조회 후 hasNext 판단 로직

- [x] **M1-010** — Git Hooks 설치
  - `cp .claude/hooks/pre-commit-check.sh .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit`
  - `cp .claude/hooks/commit-msg-check.sh .git/hooks/commit-msg && chmod +x .git/hooks/commit-msg`
  - 커밋 시 자동 검증: else 키워드, System.out.println, 하드코딩 시크릿, Conventional Commits 형식
  - docs/ 또는 .claude/ 파일 스테이징 시 `doc-consistency-check.sh` 자동 실행 (정책 정합성 검증)

- [x] **M1-011** — 통합 확인 + 첫 커밋
  - `./gradlew bootRun` → 8080 기동 성공
  - `/actuator/health` → 200 OK + MySQL/Redis 연결 확인
  - git init → 첫 커밋: `chore: initialize SnapStock project`

---

## M2: 인증/인가 (Week 2)

> **목표**: 회원가입 → 로그인 → 토큰 재발급 → 로그아웃 전체 플로우 완성
> **완료 기준**: 통합 테스트에서 전체 인증 플로우 통과

### User 엔티티 + 회원가입

- [x] **M2-001** — User 엔티티 생성 `@planner`
  - `domain/user/entity/User.java`
  - 필드: `id`, `email`, `password`, `nickname`, `role`(enum USER/ADMIN), `deletedAt`
  - `BaseEntity` 상속 (createdAt 자동)
  - `role` 기본값: `Role.USER`

- [x] **M2-002** — Role enum
  - `domain/user/entity/Role.java`
  - `USER`, `ADMIN`
  - Spring Security `GrantedAuthority` 매핑: `ROLE_USER`, `ROLE_ADMIN`

- [x] **M2-003** — UserRepository
  - `domain/user/repository/UserRepository.java`
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
  - `boolean existsByNickname(String nickname)`

- [x] **M2-004** — 회원가입 API
  - `SignUpRequest` record: `@Email email`, `@NotBlank password`(8~20자), `@NotBlank nickname`
  - `UserService.signUp()`: 이메일 중복 체크 → 닉네임 중복 체크 → BCrypt 암호화 → 저장
  - `AuthController.signUp()`: `POST /api/v1/auth/signup` → 201 Created
  - ErrorCode 추가: `DUPLICATE_EMAIL(409)`, `DUPLICATE_NICKNAME(409)`

- [x] **M2-005** — 회원가입 테스트
  - Unit: `UserServiceTest` — 정상가입, 이메일중복_예외발생
  - API: `AuthControllerTest` — 201 응답, 400 Validation 실패, 409 중복

### JWT 인증

- [x] **M2-006** — JwtTokenProvider 구현 `@planner`
  - `global/auth/JwtTokenProvider.java`
  - Access Token 생성 (30분, userId + role claim)
  - Refresh Token 생성 (14일, userId claim)
  - 토큰 파싱 + 유효성 검증
  - `resolveToken(HttpServletRequest)`: Bearer 헤더 추출
  - JWT Secret: `application.yml`의 `jwt.secret` (환경변수 바인딩, fail-fast 검증)

- [x] **M2-007** — RedisConfig + 토큰 저장소
  - `global/config/RedisConfig.java`
  - `StringRedisTemplate` Bean (토큰 저장용, Spring Boot 자동 구성 사용)
  - Refresh Token 저장: `refresh:{userId}` → `refreshToken` (TTL 14일)
  - Access Token 블랙리스트: `blacklist:{sha256(accessToken)}` → `"true"` (TTL = 잔여 만료시간). 토큰 원문 대신 SHA-256 해시를 키로 사용하여 Redis 메모리 절감 및 토큰 노출 방지
  - 브라우저 클라이언트 기준 Refresh Token 전달: 쿠키 (정책: PRD §4.6 로그인 응답 참조)

- [x] **M2-008** — JwtAuthenticationFilter
  - `global/auth/JwtAuthenticationFilter.java`
  - `OncePerRequestFilter` 상속
  - 토큰 추출 → 유효성 검증 → 블랙리스트 체크 → `SecurityContextHolder` 설정
  - `UserPrincipal` record: `userId`, `role` 보유

- [x] **M2-009** — SecurityConfig
  - `global/config/SecurityConfig.java`
  - CSRF disable, SessionCreationPolicy.STATELESS
  - `cors(withDefaults())` + `CorsConfigurationSource` 구성 (`allowedOrigins` 화이트리스트, `Authorization`/`Idempotency-Key` 헤더 허용)
  - `allowCredentials=false` 명시
  - 엔드포인트별 접근 정책: PRD §5.1.3 참조 (permitAll / authenticated / hasRole("ADMIN") 규칙 정본)
  - ⚠️ reissue는 Access Token 만료 상태에서 호출되므로 permitAll. 인증은 서비스 레이어에서 Refresh Token 검증으로 대체
  - `JwtAuthenticationFilter` 등록
  - `PasswordEncoder`: BCrypt(BCRYPT_STRENGTH = 10)

### 로그인/로그아웃/재발급

- [x] **M2-010** — 로그인 API
  - `LoginRequest` record: `@Email email`, `@NotBlank password`
  - `LoginResponse` record: `accessToken`, `refreshToken`, `tokenType`("Bearer"), `expiresIn`(초 단위, 1800)
  - `AuthService.login()`: 이메일 조회 → 비밀번호 BCrypt 매칭 → 토큰 생성 → Refresh Redis 저장 (기본: body 응답)
  - 브라우저/Swagger 요청 시 Refresh Token 쿠키 발급 허용 (정책: PRD §4.6 로그인 응답 참조)
  - `AuthController.login()`: `POST /api/v1/auth/login` → 200 OK
  - ErrorCode 추가: `LOGIN_FAILED(401)`

- [x] **M2-011** — 토큰 재발급 API
  - `TokenReissueRequest` record: `String refreshToken` (body 전달 클라이언트용, nullable 허용)
  - `AuthService.reissue()`: `body.refreshToken` 우선, body 미존재 시 cookie fallback → 검증 → Redis 일치 확인 → 새 토큰 쌍 발급 → 이전 Refresh 삭제 (Rotation)
  - 입력 누락 시 `400 INVALID_INPUT`, 무효/만료 시 `401 INVALID_REFRESH_TOKEN`
  - `AuthController.reissue()`: `POST /api/v1/auth/reissue` → 200 OK
  - ErrorCode 추가: `INVALID_REFRESH_TOKEN(401)`

- [x] **M2-012** — 로그아웃 API
  - `AuthService.logout()`: Access Token 블랙리스트 등록 + Refresh Token 삭제
  - 쿠키 모드 사용 시 Refresh 쿠키 `Max-Age=0` 만료 처리
  - `AuthController.logout()`: `POST /api/v1/auth/logout` → 204 No Content
  - 블랙리스트 TTL = Access Token 잔여 만료시간

- [ ] **M2-013** — 인증 통합 테스트 `@test-engineer`
  - 전체 플로우: 가입 → 로그인 → 인증 API 호출 → 토큰 재발급 → 로그아웃 → 블랙리스트 확인
  - Testcontainers: MySQL + Redis
  - 만료된 토큰 거부, 블랙리스트 토큰 거부, 잘못된 토큰 형식 거부
  - 브라우저 쿠키 기반 Refresh 재발급 플로우 검증
  - `reissue` 입력 우선순위 검증(body 우선, cookie fallback)
  - 쿠키 속성 검증: PRD §4.6 정책과 동일한 속성값인지 확인

### 회원 프로필

- [ ] **M2-014** — 내 정보 조회 API
  - `UserResponse` record: `userId`, `email`, `nickname`, `role`, `createdAt`
  - `UserService.getMyInfo(Long userId)`
  - `UserController.getMyInfo()`: `GET /api/v1/users/me` → 200 OK
  - `@AuthenticationPrincipal UserPrincipal` 사용

- [ ] **M2-015** — 내 정보 수정 API
  - `UserUpdateRequest` record: `nickname`(Optional), `password`(Optional, 8~20자)
  - `UserService.updateMyInfo()`: 닉네임 변경 시 중복 체크 → 변경 필드만 업데이트
  - `UserController.updateMyInfo()`: `PATCH /api/v1/users/me` → 200 OK

- [ ] **M2-016** — 회원 탈퇴 API (Soft Delete)
  - `User.softDelete()`: `this.deletedAt = LocalDateTime.now()`
  - `UserService.deleteMyAccount()`: Soft Delete + Refresh Token 삭제
  - `UserController.deleteMyAccount()`: `DELETE /api/v1/users/me` → 204 No Content
  - 탈퇴 유저 로그인 시 `DELETED_USER(401)` 에러

- [ ] **M2-017** — 회원 API 테스트 `@test-engineer`
  - Unit: `UserServiceTest` — 조회, 수정, 탈퇴 각 정상/실패 케이스
  - API: `UserControllerTest` — 인증 없이 접근 시 401, 정상 조회 200

---

## M3: 상품 도메인 (Week 3)

> **목표**: 상품 CRUD 완성 (Admin 생성/수정/삭제 + 공개 조회)
> **완료 기준**: 전체 상품 API 테스트 통과 + 커서 페이지네이션 동작

### 엔티티 + Repository

- [ ] **M3-001** — Product 엔티티 `@planner`
  - `domain/product/entity/Product.java`
  - 필드: `id`, `name`, `description`, `originalPrice`(int), `stock`(int), `category`
  - `BaseEntity` 상속 (createdAt, updatedAt)
  - `deletedAt`: Soft Delete (`DATETIME(6) NULLABLE`)
  - 비즈니스 메서드: `update(name, description, price, stock, category)`, `softDelete()`

- [ ] **M3-002** — ProductRepository
  - `domain/product/repository/ProductRepository.java`
  - 커서 페이지네이션: `findByCategoryAndIdLessThan(category, cursor, Pageable)`
  - 전체 조회용: `findByIdLessThan(cursor, Pageable)`
  - 첫 페이지: `findByCategory(category, Pageable)` / `findAll(Pageable)` — cursor 없을 때

### Admin API (생성/수정/삭제)

- [ ] **M3-003** — 상품 등록 API
  - `ProductCreateRequest` record: `@NotBlank name`, `description`, `@Positive originalPrice`, `@PositiveOrZero stock`, `@NotBlank category`
  - `ProductService.createProduct(request)`: 엔티티 생성 → 저장
  - `AdminProductController.create()`: `POST /api/v1/admin/products` → 201 Created

- [ ] **M3-004** — 상품 수정 API
  - `ProductUpdateRequest` record: `name`, `description`, `originalPrice`, `stock`, `category` — 모두 Optional(null 허용)
  - `ProductService.updateProduct(id, request)`: 기존 엔티티 조회 → 변경 필드만 업데이트
  - `AdminProductController.update()`: `PATCH /api/v1/admin/products/{id}` → 200 OK
  - ErrorCode 추가: `PRODUCT_NOT_FOUND(404)`

- [ ] **M3-005** — 상품 삭제 API (Soft Delete)
  - `ProductService.deleteProduct(id)`: 존재 확인 → OPEN/UPCOMING 타임딜 체크 → Soft Delete (`deletedAt = now()`)
  - `AdminProductController.delete()`: `DELETE /api/v1/admin/products/{id}` → 204 No Content
  - OPEN 또는 UPCOMING 상태 타임딜 존재 시 삭제 불가 → `PRODUCT_HAS_ACTIVE_DEAL(409)`

### 공개 조회 API

- [ ] **M3-006** — 상품 목록 조회 (커서 페이지네이션)
  - `ProductListResponse` record: `productId`, `name`, `originalPrice`, `category`
  - `ProductService.getProducts(category, cursor, size)`: 커서 기반 조회 → CursorPageResponse 생성
  - `ProductController.getProducts()`: `GET /api/v1/products?category=&cursor=&size=10` → 200 OK
  - `size + 1` 조회 후 hasNext 판단

- [ ] **M3-007** — 상품 상세 조회
  - `ProductDetailResponse` record: `productId`, `name`, `description`, `originalPrice`, `stock`, `category`, `createdAt`
  - `ProductService.getProduct(id)`: 단건 조회
  - `ProductController.getProduct()`: `GET /api/v1/products/{id}` → 200 OK

- [ ] **M3-008** — 인덱스 생성 + EXPLAIN 검증
  - `idx_products_category_id`: `(category, id DESC)`
  - EXPLAIN ANALYZE로 인덱스 사용 확인

### 테스트

- [ ] **M3-009** — 상품 서비스 Unit 테스트 `@test-engineer`
  - 생성 정상, 수정 정상, 삭제 정상
  - 조회 시 NOT_FOUND 예외
  - 커서 페이지네이션 hasNext 판단 로직

- [ ] **M3-010** — 상품 컨트롤러 API 테스트 `@test-engineer`
  - Admin API: ADMIN 권한 필요 (USER → 403, 미인증 → 401)
  - 공개 API: 인증 없이 접근 가능
  - 201, 200, 204, 400, 404 응답 확인

---

## M4: 타임딜 도메인 (Week 4)

> **목표**: 타임딜 CRUD + 상태 전이 스케줄러 완성
> **완료 기준**: UPCOMING → OPEN → CLOSED 자동 전이 + 전체 API 테스트 통과

### 엔티티 + 상태 관리

- [ ] **M4-001** — DealStatus enum `@planner`
  - `domain/timedeal/entity/DealStatus.java`
  - `UPCOMING`, `OPEN`, `CLOSED`

- [ ] **M4-002** — TimeDeal 엔티티
  - `domain/timedeal/entity/TimeDeal.java`
  - 필드: `id`, `product`(ManyToOne LAZY), `dealPrice`, `dealStock`, `remainingStock`, `startTime`, `endTime`, `status`, `version`(@Version)
  - `BaseEntity` 상속
  - 비즈니스 메서드:
    - `validateOpen()`: OPEN 아니면 CustomException
    - `validateStockAvailable(int qty)`: 잔여수량 부족 시 CustomException
    - `deductStock(int qty)`: `this.remainingStock -= qty`
    - `restoreStock(int qty)`: `this.remainingStock += qty`
    - `isAvailable()`: `status == OPEN && remainingStock > 0`

- [ ] **M4-003** — TimeDealRepository
  - 기본 CRUD + 상태별 조회
  - `List<TimeDeal> findByStatus(DealStatus status)` + `@EntityGraph(attributePaths = "product")`
  - 커서 페이지네이션: `findByStatusAndIdLessThan(status, cursor, Pageable)`
  - 비관적 락: `findByIdForUpdate(Long id)` — `@Lock(PESSIMISTIC_WRITE)`
  - 벌크 상태 전이: `openDeals(@Param("now") LocalDateTime)`, `closeDeals(@Param("now") LocalDateTime)`

### Admin API

- [ ] **M4-004** — 타임딜 생성 API
  - `TimeDealCreateRequest` record: `@NotNull productId`, `@Positive dealPrice`, `@Positive dealStock`, `@Future startTime`, `@Future endTime`
  - Validation: `startTime < endTime`, `dealPrice < product.originalPrice`
  - `TimeDealService.createDeal(request)`: Product 조회 → TimeDeal 생성 (UPCOMING)
  - `AdminTimeDealController.create()`: `POST /api/v1/admin/time-deals` → 201 Created
  - ErrorCode 추가: `DEAL_INVALID_TIME(400)`, `DEAL_PRICE_TOO_HIGH(400)`

- [ ] **M4-005** — 타임딜 수정 API
  - `TimeDealUpdateRequest` record: `dealPrice`, `dealStock`, `startTime`, `endTime` — UPCOMING일 때만 수정 가능
  - `TimeDealService.updateDeal(id, request)`: 상태 확인 → 수정
  - `AdminTimeDealController.update()`: `PATCH /api/v1/admin/time-deals/{id}` → 200 OK
  - ErrorCode 추가: `DEAL_NOT_MODIFIABLE(409)`

- [ ] **M4-006** — 타임딜 삭제 API
  - UPCOMING일 때만 삭제 가능
  - `AdminTimeDealController.delete()`: `DELETE /api/v1/admin/time-deals/{id}` → 204 No Content

### 공개 조회 API

- [ ] **M4-007** — 타임딜 목록 조회
  - `TimeDealListResponse` record: `dealId`, `productName`, `originalPrice`, `dealPrice`, `discountRate`, `remainingStock`, `dealStock`, `startTime`, `endTime`, `status`
  - 상태 필터: `GET /api/v1/time-deals?status=OPEN&cursor=&size=10`
  - `@EntityGraph` Product fetch join → N+1 방지

- [ ] **M4-008** — 타임딜 상세 조회
  - `TimeDealDetailResponse` record: 위 + `productDescription`, `category`
  - `GET /api/v1/time-deals/{id}` → 200 OK

### 스케줄러

- [ ] **M4-009** — TimeDealScheduler 구현 `@planner`
  - `domain/timedeal/scheduler/TimeDealScheduler.java`
  - `@Scheduled(fixedRate = 60000)`: 1분마다 실행
  - 벌크 JPQL: `UPDATE TimeDeal SET status = 'OPEN' WHERE status = 'UPCOMING' AND startTime <= :now`
  - 벌크 JPQL: `UPDATE TimeDeal SET status = 'CLOSED' WHERE status = 'OPEN' AND (endTime <= :now OR remainingStock = 0)`
  - `@Modifying(clearAutomatically = true)` 사용
  - 로그: 전이된 건수 INFO 출력
  - ⚠️ Phase 4(Redis): DB `remainingStock`은 주문 시 동기 차감되므로 스케줄러 조건은 유효. 단, Redis 재고 0 시점과 스케줄러 주기(최대 59초) 사이 지연이 있을 수 있음

- [ ] **M4-010** — 인덱스 생성 + EXPLAIN 검증
  - `idx_time_deals_status_id`: `(status, id DESC)` (목록/커서)
  - `idx_time_deals_status_start`: `(status, start_time)` (UPCOMING → OPEN)
  - `idx_time_deals_status_end`: `(status, end_time)` (OPEN → CLOSED)
  - 스케줄러 쿼리 EXPLAIN 확인

### 테스트

- [ ] **M4-011** — 타임딜 서비스 Unit 테스트 `@test-engineer`
  - CRUD 정상/실패 케이스
  - OPEN 상태 수정 시도 → DEAL_NOT_MODIFIABLE 예외
  - `validateOpen()`, `validateStockAvailable()`, `deductStock()` 도메인 메서드 테스트

- [ ] **M4-012** — 타임딜 스케줄러 통합 테스트 `@test-engineer`
  - Testcontainers + `@SpringBootTest`
  - UPCOMING 딜 생성 → startTime 과거로 설정 → 스케줄러 수동 호출 → OPEN 확인
  - OPEN 딜 → endTime 과거로 설정 → 스케줄러 수동 호출 → CLOSED 확인

- [ ] **M4-013** — 타임딜 API 테스트 `@test-engineer`
  - Admin: 권한 검증, 201/200/204/400/409
  - 공개: N+1 방지 확인 (쿼리 로그 체크)

---

## M5: 주문 도메인 — 기본 구현 (Week 5) ⚡

> **목표**: 주문 CRUD + Phase 1(synchronized) 동시성 제어 완성
> **완료 기준**: 동시성 테스트에서 synchronized 정확성 확인 (oversell = 0)

### 엔티티 + 기본 구조

- [ ] **M5-001** — OrderStatus enum `@planner`
  - `CREATED`, `PAID`, `CANCELLED`

- [ ] **M5-002** — Order 엔티티
  - `domain/order/entity/Order.java`
  - 필드: `id`, `userId`(Long), `timeDealId`(Long), `quantity`, `totalPrice`, `status`
  - `BaseEntity` 상속
  - 비즈니스 메서드: `pay()` (CREATED → PAID 전이, 이미 PAID면 `ORDER_ALREADY_PAID` 예외)
  - 비즈니스 메서드: `cancel()` (CREATED/PAID → CANCELLED 전이, 이미 CANCELLED면 no-op으로 멱등 처리)
  - Unique Constraint: `(user_id, time_deal_id)` — 중복 주문 방지

- [ ] **M5-003** — OrderRepository
  - `Optional<Order> findByUserIdAndTimeDealId(Long userId, Long timeDealId)` — 중복 체크
  - 커서 페이지네이션: `findByUserIdAndIdLessThan(userId, cursor, Pageable)`
  - 인덱스: `idx_orders_user_id`, `idx_orders_time_deal_status`, `ux_orders_user_deal`

### StockService 인터페이스 + Phase 1

- [ ] **M5-004** — StockService 인터페이스 (전략 패턴) ⚡ `@planner`
  - `domain/order/service/stock/StockService.java`
  - `StockDeductionResult deductStock(Long timeDealId, int quantity)`
  - `void restoreStock(Long timeDealId, int quantity)`
  - `StockDeductionResult` record: `boolean success`, `int remainingStock`

- [ ] **M5-005** — Phase 1: SyncStockService (synchronized) ⚡
  - `domain/order/service/stock/SyncStockService.java`
  - `@Profile("sync")` 또는 `@ConditionalOnProperty("snapstock.stock.strategy", havingValue = "sync")`
  - `synchronized` 메서드 → TimeDeal 조회 → validate → deductStock → saveAndFlush
  - 단일 JVM 한정, 성능 기준점(baseline)

- [ ] **M5-006** — OrderService 구현
  - `domain/order/service/OrderService.java`
  - `createOrder(Long userId, OrderCreateRequest)`:
    - 중복 주문 체크 → `DUPLICATE_ORDER(409)`
    - TimeDeal 조회 → 상태 검증
    - 경계 시간 검증: `startTime <= now < endTime` (스케줄러 지연 보정)
    - `stockService.deductStock()` 호출
    - Order 엔티티 생성 → 저장
    - ⚠️ Phase 4(Redis) 한정: DB 저장 실패 시 Redis 재고 보상 복구 (`INCRBY`) 처리 — M6-007에서 구현
  - `cancelOrder(Long userId, Long orderId)`: 본인 확인 → 취소 → `stockService.restoreStock()`
  - `payOrder(Long userId, Long orderId, String idempotencyKey)`: 멱등키 기반 중복 결제 방지
  - `getMyOrders(userId, cursor, size)`: 커서 페이지네이션
  - `getOrder(userId, orderId)`: 단건 조회 + 본인 확인

### 주문 API

- [ ] **M5-007** — 주문 생성 API ⚡
  - `OrderCreateRequest` record: `@NotNull timeDealId`, `@Positive quantity`
  - `OrderController.createOrder()`: `POST /api/v1/orders` → 201 Created
  - `OrderResponse` record: `orderId`, `timeDealId`, `productName`, `quantity`, `totalPrice`, `status`, `createdAt`
  - ErrorCode 추가: `DEAL_NOT_FOUND(404)`, `DEAL_NOT_OPEN(409)`, `DEAL_STOCK_EXHAUSTED(409)`, `DUPLICATE_ORDER(409)`

- [ ] **M5-008** — 주문 결제/취소 API (멱등성)
  - `OrderController.payOrder()`: `POST /api/v1/orders/{id}/pay` → 200 OK (`Idempotency-Key` 필수)
  - `OrderController.cancelOrder()`: `POST /api/v1/orders/{id}/cancel` → 200 OK (`Idempotency-Key` 필수)
  - 동일 `Idempotency-Key` 재요청 시 동일 응답 재반환
  - 이미 `CANCELLED` 상태인 주문은 no-op + 200 응답(멱등)
  - 이미 `PAID` 상태 주문 재결제 시 `409 ORDER_ALREADY_PAID` 반환
  - 취소된 주문에 결제 시도 시 `409 ORDER_ALREADY_CANCELLED` 반환
  - `PAID` 상태 주문도 취소 가능 (재고 복구). 이미 `CANCELLED` 상태면 no-op + 200 응답(멱등) — 이 경우 `ORDER_ALREADY_CANCELLED(409)`를 반환하지 않음
  - ErrorCode 추가: `ORDER_NOT_FOUND(404)`, `ORDER_ALREADY_PAID(409)`, `ORDER_ALREADY_CANCELLED(409)`, `IDEMPOTENCY_KEY_REQUIRED(400)`

- [ ] **M5-009** — 내 주문 목록 조회 API
  - `OrderController.getMyOrders()`: `GET /api/v1/orders?cursor=&size=10` → 200 OK
  - CursorPageResponse 적용

- [ ] **M5-010** — 주문 상세 조회 API
  - `OrderController.getOrder()`: `GET /api/v1/orders/{id}` → 200 OK
  - 본인 주문만 조회 가능 (userId 불일치 시 FORBIDDEN)

### 테스트

- [ ] **M5-011** — OrderService Unit 테스트 `@test-engineer`
  - 정상 주문 생성, 중복 주문 예외, 재고 부족 예외, OPEN 아닌 딜 예외
  - CREATED 주문 취소 정상, PAID 주문 취소 정상 (재고 복구 확인)
  - 이미 취소된 주문 재요청 시 상태 유지(멱등) 검증
  - 결제 멱등키 재요청 시 중복 결제 미발생 검증
  - 본인 주문 아닌 경우 FORBIDDEN

- [ ] **M5-012** — Phase 1 동시성 테스트 ⚡ `@test-engineer`
  - `SyncStockServiceConcurrencyTest`
  - 100개 재고 + 1000 스레드 동시 요청
  - `ExecutorService` + `CountDownLatch` 패턴
  - 검증: `success.get() == 100`, `fail.get() == 900`
  - 이 테스트가 모든 Phase에서 재사용됨

- [ ] **M5-013** — 주문 API 테스트 `@test-engineer`
  - MockMvc: 201, 200, 400, 401, 404, 409 응답 확인
  - `POST /api/v1/orders/{id}/pay`에서 `Idempotency-Key` 누락 시 `400 IDEMPOTENCY_KEY_REQUIRED` 검증
  - `POST /api/v1/orders/{id}/cancel`에서 `Idempotency-Key` 누락 시 `400 IDEMPOTENCY_KEY_REQUIRED` 검증
  - `POST /api/v1/orders/{id}/pay`에서 이미 `PAID` 주문 재결제 시 `409 ORDER_ALREADY_PAID` 검증
  - 인증 필수 검증

- [ ] **M5-014** — 인덱스 + EXPLAIN 검증
  - `idx_orders_user_id`, `idx_orders_time_deal_status`, `ux_orders_user_deal`
  - 주문 목록/상세 쿼리 EXPLAIN 확인

---

## M6: 동시성 제어 — Phase 2~4 (Week 6) ⚡

> **목표**: 비관적 락 → 낙관적 락 → Redis Lua Script 순서대로 구현 + 비교 리포트
> **완료 기준**: 4단계 모두 oversell 0 + 단계별 성능 차이 측정

### Phase 2: Pessimistic Lock

- [ ] **M6-001** — PessimisticStockService 구현 ⚡ `@planner`
  - `domain/order/service/stock/PessimisticStockService.java`
  - `@ConditionalOnProperty("snapstock.stock.strategy", havingValue = "pessimistic")`
  - `@Transactional(timeout = 5)` 필수 — 데드락 방지
  - `timeDealRepository.findByIdForUpdate(id)` → SELECT FOR UPDATE
  - 재고 차감 → save

- [ ] **M6-002** — Phase 2 동시성 테스트 ⚡ `@test-engineer`
  - M5-012 동일 패턴 재사용 (프로파일만 변경)
  - 추가 검증: HikariCP `hikaricp.connections.active` 메트릭 로그 확인
  - 기대: oversell = 0이지만 TPS가 낮음

### Phase 3: Optimistic Lock

- [ ] **M6-003** — @Retryable 설정 `@planner`
  - `build.gradle`: `spring-retry` 의존성 추가
  - `@EnableRetry` 설정 클래스
  - 또는 직접 retry 루프 구현 (의존성 최소화)

- [ ] **M6-004** — OptimisticStockService 구현 ⚡
  - `domain/order/service/stock/OptimisticStockService.java`
  - `@ConditionalOnProperty("snapstock.stock.strategy", havingValue = "optimistic")`
  - `@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))`
  - TimeDeal의 `@Version` 필드 활용

- [ ] **M6-005** — Phase 3 동시성 테스트 ⚡ `@test-engineer`
  - 동일 패턴, 프로파일 변경
  - 추가 검증: 재시도 횟수 로그, 최종 재고 정합성

### Phase 4: Redis Lua Script

- [ ] **M6-006** — Redis 재고 초기화 로직 ⚡ `@planner`
  - 타임딜 OPEN 시 Redis에 재고 로드: `deal:stock:{dealId}` = DB `remaining_stock`
  - `TimeDealScheduler` 또는 별도 서비스에서 호출
  - 키 TTL: `endTime - now + 1시간` (안전장치, CLOSED 전이 시 명시적 삭제가 우선)
  - 딜 종료(CLOSED) 시 Redis 최종값을 DB `remaining_stock`에 동기화 후 키 삭제

- [ ] **M6-007** — RedisStockService + Lua Script 구현 ⚡
  - `domain/order/service/stock/RedisStockService.java`
  - `@ConditionalOnProperty("snapstock.stock.strategy", havingValue = "redis")`
  - Lua Script: GET → nil/부족 체크 → `DECRBY quantity` (원자적)
  - `RedisScript<Long>` 빈 등록
  - restoreStock: `INCRBY quantity` (취소/보상 시)
  - DB 저장 실패 보상 경로: Redis 재고 즉시 복구 + 재처리 로그 적재

- [ ] **M6-008** — Phase 4 동시성 테스트 ⚡ `@test-engineer`
  - Testcontainers Redis 사용
  - 동일 패턴 + Redis 재고 초기화 포함
  - 기대: oversell = 0, TPS 최고, DB connection 사용 없음

### 비교 리포트

- [ ] **M6-009** — 4단계 성능 비교 리포트 작성 ⚡ `@performance-analyzer`
  - 각 Phase별 측정:
    - oversell 카운트 (0 필수)
    - 테스트 소요 시간
    - 에러율
  - 비교 표 작성 (README 또는 docs/CONCURRENCY_REPORT.md)
  - 결론: 왜 Redis `DECRBY` + 보상 처리 전략이 최종 운영안인지 근거 정리

- [ ] **M6-010** — StockService 전략 전환 설정 정리
  - `application.yml`에 `snapstock.stock.strategy: redis` 기본값 설정
  - 프로파일별 전환 방법 README에 문서화

---

## M7: Redis 캐싱 + DB 최적화 (Week 7)

> **목표**: Cache Aside 적용 + 쿼리 튜닝 + 캐싱 전후 비교
> **완료 기준**: EXPLAIN 분석 기록 + 캐싱 전후 응답시간 비교 완성

### Redis 캐싱 적용

- [ ] **M7-001** — RedisConfig 캐시용 설정 확장 `@planner`
  - `RedisTemplate<String, Object>` Bean (`GenericJackson2JsonRedisSerializer`)
  - ⚠️ `activateDefaultTyping`은 역직렬화 공격면이 열리므로 사용 금지 — `GenericJackson2JsonRedisSerializer`가 내부적으로 안전한 타입 매핑을 제공
  - 캐시 DTO에 `@JsonTypeInfo` 미사용, 직렬화/역직렬화 대상 클래스를 명시적으로 제한
  - 기존 StringRedisTemplate (토큰)과 분리

- [ ] **M7-002** — 타임딜 목록 캐싱 (Cache Aside)
  - 키: `cache:deals:open`, TTL: 30초
  - `TimeDealService.getOpenDeals()`: Redis 조회 → miss 시 DB 조회 → Redis 저장
  - 스케줄러 상태 전이 시 캐시 무효화

- [ ] **M7-003** — 타임딜 상세 캐싱 (Cache Aside)
  - 키: `cache:deal:{id}`, TTL: 60초
  - 상세 조회 시 캐싱
  - 딜 수정/상태 전이 시 무효화

- [ ] **M7-004** — 상품 상세 캐싱 (Cache Aside)
  - 키: `cache:product:{id}`, TTL: 5분
  - 상품 수정 시 무효화

- [ ] **M7-005** — 캐시 무효화 이벤트 연동
  - 상태 전이 시: `cache:deals:open` + `cache:deal:{id}` 삭제
  - 상품 수정 시: `cache:product:{id}` 삭제
  - 주문 시 재고 변경: Redis 재고는 별도 관리 (Lua Script)

### DB 쿼리 최적화

- [ ] **M7-006** — 전체 쿼리 EXPLAIN 분석 `@performance-analyzer`
  - 타임딜 목록 조회 (status 필터)
  - 주문 목록 조회 (userId + cursor)
  - 상품 목록 조회 (category + cursor)
  - 스케줄러 벌크 UPDATE
  - 각 쿼리의 EXPLAIN ANALYZE 결과 기록

- [ ] **M7-007** — 누락 인덱스 추가 (발견 시)
  - M7-006 분석 결과에 따라 인덱스 추가/수정
  - 변경 전후 EXPLAIN 비교 기록

- [ ] **M7-008** — fetch join / @EntityGraph 점검
  - 모든 목록 API에서 N+1 발생하지 않는지 확인
  - `spring.jpa.show-sql: true` + 쿼리 카운트 로그

### 성능 비교 문서

- [ ] **M7-009** — 캐싱 전후 응답시간 비교 `@performance-analyzer`
  - 타임딜 목록: 캐싱 전 vs 후
  - 타임딜 상세: 캐싱 전 vs 후
  - 상품 상세: 캐싱 전 vs 후
  - docs/CACHING_REPORT.md 작성

- [ ] **M7-010** — 캐싱 통합 테스트 `@test-engineer`
  - Cache miss → DB 조회 확인
  - Cache hit → DB 미조회 확인
  - 무효화 후 재조회 → 최신 데이터 확인
  - Testcontainers Redis 사용

---

## M8: 비동기 이벤트 + 부하 테스트 (Week 8)

> **목표**: 이벤트 기반 분리 + k6 부하 테스트 리포트 완성
> **완료 기준**: k6 리포트 (TPS, p95, 에러율) 완성

### Spring Event 기반 비동기 처리

- [ ] **M8-001** — DomainEventPublisher 인터페이스 + 구현 `@planner`
  - `global/common/DomainEventPublisher.java` (인터페이스)
  - `global/common/SpringEventPublisher.java` (구현체)
  - `ApplicationEventPublisher` 위임
  - 향후 Kafka 전환 시 구현체만 교체

- [ ] **M8-002** — OrderCreatedEvent
  - `domain/order/event/OrderCreatedEvent.java` record
  - 필드: `orderId`, `userId`, `timeDealId`, `quantity`, `totalPrice`

- [ ] **M8-003** — OrderEventListener
  - `domain/order/event/OrderEventListener.java`
  - `@TransactionalEventListener(phase = AFTER_COMMIT)`: 주문 이력 로깅
  - `@Async @EventListener`: 알림 전송 (현재는 로그 출력만)

- [ ] **M8-004** — AsyncConfig
  - `global/config/AsyncConfig.java`
  - `@EnableAsync`
  - ThreadPoolTaskExecutor: core 2, max 5, queue 100, prefix "snapstock-async-"

- [ ] **M8-005** — OrderService에 이벤트 발행 연동
  - `createOrder()` 성공 후 `eventPublisher.publish(new OrderCreatedEvent(...))`
  - 트랜잭션 커밋 후 리스너 실행 확인

- [ ] **M8-006** — 이벤트 테스트 `@test-engineer`
  - `@TransactionalEventListener` 동작 확인
  - 트랜잭션 롤백 시 이벤트 미발행 확인
  - `@Async` 비동기 실행 확인 (스레드 이름 검증)

### k6 부하 테스트

- [ ] **M8-007** — k6 테스트 스크립트 작성 `@planner`
  - `k6/order-load-test.js`: 주문 생성 API 부하 테스트
  - 시나리오: 100 VU, 1000 iterations, burst 모드
  - 인증 토큰 사전 발급 → 각 VU에 분배
  - 임계값: `http_req_duration p(95) < 200ms`, `http_req_failed rate < 0.01`

- [ ] **M8-008** — k6 테스트 스크립트: 타임딜 조회
  - `k6/deal-list-load-test.js`: 타임딜 목록 조회 부하 테스트
  - 캐싱 효과 확인용

- [ ] **M8-009** — k6 실행 + 리포트 수집
  - Phase 4(Redis) 기준 실행
  - 결과: TPS, p95, p99, error rate, avg duration
  - 스크린샷/JSON 결과 저장

- [ ] **M8-010** — 부하 테스트 리포트 문서화 `@performance-analyzer`
  - docs/LOAD_TEST_REPORT.md
  - 시나리오별 결과 표
  - 병목 분석 + 개선 방향

---

## M9: 인프라 + 문서화 + 마무리 (Week 9)

> **목표**: Docker 원커맨드 구동 + CI/CD + README 완성
> **완료 기준**: `docker-compose up` 전체 구동 + GitHub Actions 파이프라인 정상 + README 완성

### Docker

- [ ] **M9-001** — Dockerfile (Multi-stage Build) `@planner`
  - Stage 1: Gradle build (Java 21)
  - Stage 2: JRE 21 slim + JAR 복사
  - `.dockerignore` 작성

- [ ] **M9-002** — docker-compose.yml 최종 정리
  - app: Dockerfile 빌드, depends_on(mysql, redis), health check
  - mysql: 8.4, 볼륨, 초기 DDL 스크립트 마운트
  - redis: 7-alpine
  - `docker-compose up -d` → 전체 기동 확인

- [ ] **M9-003** — 초기 데이터 스크립트
  - `docker/init.sql`: DB 생성, 테이블 생성 (DDL)
  - `docker/data.sql`: 샘플 데이터 (Admin 유저, 상품 5개, 타임딜 3개)

### CI/CD

- [ ] **M9-004** — GitHub Actions 워크플로우
  - `.github/workflows/ci.yml`
  - Trigger: push/PR to `develop`, `main`
  - Steps: Checkout → JDK 21 Setup → Gradle Build + Test (Testcontainers) → 결과 리포트
  - Docker build (main 브랜치만)

- [ ] **M9-005** — GitHub Actions 동작 확인
  - PR 생성 → 파이프라인 실행 → 테스트 통과 확인
  - 실패 시 원인 분석 + 수정

### 문서화

- [ ] **M9-006** — README.md 작성 `@planner`
  - 프로젝트 소개 (한 줄 요약 + 기술 스택 뱃지)
  - 핵심 기술 (동시성 4단계 비교 표 + 성능 결과)
  - ERD (dbdiagram.io 이미지)
  - API 명세 (OpenAPI/Swagger 링크)
  - 실행 방법 (`docker-compose up` 원커맨드)
  - 트러블슈팅 (주요 이슈 + 해결)

- [ ] **M9-007** — 기술 문서 정리
  - docs/CONCURRENCY_REPORT.md (M6-009)
  - docs/CACHING_REPORT.md (M7-009)
  - docs/LOAD_TEST_REPORT.md (M8-010)
  - 각 문서 최종 검수

- [ ] **M9-008** — API 문서 생성
  - Springdoc OpenAPI (Swagger UI) 설정
  - `/swagger-ui.html` 접근 가능 확인
  - `components.securitySchemes`에 `bearerAuth` + `cookieAuth(refreshToken)` 정의
  - `POST /api/v1/auth/reissue` 문서화: `security: []`, body/cookie 입력 규약, 400/401 에러 케이스
  - Postman import(JSON/YAML)로 동일 인증 플로우 재현 확인

### 최종 점검

- [ ] **M9-009** — 전체 테스트 실행 + 커버리지 확인
  - `./gradlew test` 전체 통과
  - JaCoCo 또는 IntelliJ 커버리지 확인
  - 목표: 도메인 90%+, 서비스 80%+

- [ ] **M9-010** — 코드 품질 최종 리뷰 `@code-reviewer`
  - 전체 코드 리뷰 (PR 체크리스트 10개 항목)
  - 🔴 CRITICAL 0개 확인
  - 불필요한 TODO/FIXME 정리
  - 문서 품질 게이트: `latest`/`TBD`/`???` 잔존 여부 0건 확인

- [ ] **M9-011** — Git 태그 + 릴리스
  - `develop` → `main` 최종 머지
  - `git tag v1.0.0`
  - GitHub Release 노트 작성

---

## Issue 통계

| Milestone | 주차 | 이슈 수 | 핵심 이슈 |
|---|---|---|---|
| **M1** | Week 1 | 11 | 프로젝트 초기화 + 글로벌 모듈 |
| **M2** | Week 2 | 17 | 인증/인가 전체 플로우 |
| **M3** | Week 3 | 10 | 상품 CRUD + 커서 페이지네이션 |
| **M4** | Week 4 | 13 | 타임딜 CRUD + 스케줄러 |
| **M5** | Week 5 | 14 | 주문 기본 + Phase 1 동시성 ⚡ |
| **M6** | Week 6 | 10 | Phase 2~4 동시성 + 비교 리포트 ⚡ |
| **M7** | Week 7 | 10 | 캐싱 + DB 최적화 |
| **M8** | Week 8 | 10 | 비동기 이벤트 + k6 부하 테스트 |
| **M9** | Week 9 | 11 | 인프라 + 문서화 + 마무리 |
| **합계** | 9주 | **106** | |

---

## Label 규칙 (GitHub Issues)

| Label | 색상 | 용도 |
|---|---|---|
| `domain:user` | 🔵 Blue | User 도메인 관련 |
| `domain:product` | 🟢 Green | Product 도메인 관련 |
| `domain:timedeal` | 🟠 Orange | TimeDeal 도메인 관련 |
| `domain:order` | 🔴 Red | Order 도메인 관련 |
| `infra` | ⚫ Gray | 인프라/설정 관련 |
| `global` | 🟣 Purple | 공통 모듈 관련 |
| `type:feature` | 💙 | 새 기능 구현 |
| `type:test` | 💚 | 테스트 작성/수정 |
| `type:docs` | 📝 | 문서 작성 |
| `type:refactor` | 🔄 | 리팩토링 |
| `priority:critical` | 🔴 | 반드시 이번 주 완료 |
| `priority:high` | 🟠 | 이번 주 목표 |
| `priority:normal` | 🟡 | 다음 주 이전 완료 |
| `concurrency` | ⚡ | 동시성 제어 관련 |
| `performance` | 🚀 | 성능 최적화 관련 |

---

## Branch 네이밍 (이슈 연동)

```
feat/M1-001-project-init
feat/M2-006-jwt-token-provider
feat/M5-004-stock-service-interface
feat/M6-007-redis-lua-stock
fix/M4-009-scheduler-timezone
test/M5-012-concurrency-phase1
docs/M9-006-readme
```

## Commit 메시지 (이슈 참조)

```
feat(global): implement ApiResponse envelope #M1-006
feat(auth): implement JwtTokenProvider #M2-006
feat(order): implement Redis Lua Script stock deduction #M6-007 ⚡
test(order): add Phase 1 concurrency test (1000 threads) #M5-012
docs: write concurrency comparison report #M6-009
```
