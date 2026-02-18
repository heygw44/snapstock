# PRD: SnapStock — 타임딜 커머스 플랫폼

> *"순식간에 사라지는 재고, 그 안에서 정합성을 지키는 백엔드"*

| 항목 | 내용 |
|---|---|
| **문서 버전** | v3.0.0 |
| **작성자** | heygw44 |
| **작성일** | 2026-02-18 |
| **최종 수정일** | 2026-02-18 |
| **상태** | Draft |
| **프로젝트 유형** | 개인 포트폴리오 (백엔드 1인 개발) |
| **레포지토리** | TBD |

---

## 1. 개요 (Overview)

### 1.1 프로젝트 목적

SnapStock은 **한정 시간 내 한정 수량 상품을 파격가에 판매하는 타임딜 커머스 플랫폼**의 백엔드 시스템이다.

본 프로젝트는 주니어 백엔드 개발자로서의 기술 역량을 실증하기 위한 포트폴리오 프로젝트로, 실제 한국 커머스 시장에서 운영되는 타임딜 서비스의 핵심 도메인을 레퍼런스로 삼는다. 단순 CRUD를 넘어, **"같은 문제를 여러 방식으로 풀고 비교 분석할 수 있는 엔지니어"**임을 증명하는 것이 핵심 목표이다.

### 1.2 핵심 기술 목표

| 우선순위 | 목표 | 검증 방법 |
|---|---|---|
| P0 | 동시성 제어 — 한정 재고에 대한 다수 동시 주문 시 데이터 정합성 보장 | 단계별 락 전략 비교 + 동시성 테스트 |
| P0 | 웹 개발 기본기 — 회원가입, 인증/인가, CRUD, REST API 설계 | API 문서화 + 통합 테스트 |
| P1 | 대용량 처리 — 캐싱 전략, DB 쿼리 최적화, 비동기 이벤트 처리 | 부하 테스트 TPS 측정 + 성능 비교 리포트 |
| P1 | 테스트 전략 — 단위/통합/동시성/부하 테스트 커버리지 | 테스트 코드 + k6 리포트 |
| P2 | 인프라 — 컨테이너화, CI/CD 파이프라인, 모니터링 | Docker Compose + GitHub Actions |

### 1.3 프로젝트 레퍼런스

#### 실서비스 레퍼런스

| 서비스 | 참고 포인트 |
|---|---|
| 쿠팡 타임딜 | 한정 시간 + 한정 수량 + 선착순 구매 모델 |
| 11번가 타임딜 | 시간대별 딜 오픈/마감 상태 전이 |
| 무신사 무진장 | 대규모 트래픽 집중 시나리오 |
| 토스 라이브쇼핑 | 피크 시간대 분당 수십만 동시 접속, 초당 수십만 건 API 요청 처리 |

#### 기술 블로그 레퍼런스

본 프로젝트의 기술적 의사결정은 국내 IT 기업 기술 블로그의 실무 사례를 적극 참고하여 설계하였다.

| 출처 | 아티클 | 반영 포인트 |
|---|---|---|
| **우아한형제들** | 빼빼로데이 선착순 이벤트 장애 회고 | Redis 카운터 + Replication 구조에서 초과 발급 문제 → Master 단일 노드 INCR로 정합성 확보. 본 프로젝트 Redis Lua Script 설계의 근거 |
| **우아한형제들** | 배민스토어 이벤트 기반 아키텍처 | Kafka + DynamoDB + Redis 조합, Zero Payload 방식 이벤트 → 본 프로젝트의 `ApplicationEvent` 기반 느슨한 결합 설계 및 향후 Kafka 전환 인터페이스 추상화 근거 |
| **여기어때** | Redis & Kafka를 활용한 선착순 쿠폰 이벤트 | Redis INCR로 동시성 해결 + Kafka로 DB Write 분리 → Write DB 부하 격리 패턴. 본 프로젝트 Phase 4 Redis 재고 차감 + 비동기 주문 저장 설계 근거 |
| **쿠팡** | 대용량 트래픽 처리를 위한 백엔드 전략 | 마이크로서비스별 데이터 분리, Read-through 캐시 + 실시간 스트리밍 캐시 이원화. 재고는 초 단위 실시간 업데이트 필요 → 본 프로젝트 캐싱 전략(일반 데이터 TTL vs 재고 실시간)의 근거 |
| **토스** | 가장 많은 트래픽을 받는 서비스의 서버 관리 전략 | 모니터링 → 병목 식별 → 최적화 → Canary 배포 이터레이션. Local Cache + 비동기 Flush 패턴 → 본 프로젝트 성능 최적화 사이클 및 모니터링 설계 근거 |
| **사람인** | ORM 사용 시 HikariCP 장애 회고 | 예상치 못한 대량 쿼리 → Heap OOM → GC STW → 커넥션 타임아웃 연쇄 장애. HikariCP leak detection 설정 → 본 프로젝트 커넥션 풀 설정 및 모니터링 설계 근거 |
| **우아한테크캠프 7기** | Team7-ELEVEN 실시간 가격 하락 경매 | 동시성 제어 및 사용자 요청 신뢰적 처리 → 본 프로젝트 4단계 락 전략 비교 구조의 영감 |

---

## 2. 기술 스택 (Tech Stack)

### 2.1 Core

| 구분 | 기술 | 버전 | 비고 |
|---|---|---|---|
| **Language** | Java | 21 (LTS) | Record, Pattern Matching, Virtual Threads 활용 |
| **Framework** | Spring Boot | 3.5.10 | Spring Framework 6.2 기반, Jakarta EE 10 |
| **Build** | Gradle (Groovy DSL) | 8.14+ | Spring Boot 3.5 공식 지원 |
| **ORM** | Spring Data JPA | 2025.0 (managed) | Hibernate 6.6 |
| **Security** | Spring Security | 6.5 (managed) | JWT 기반 인증/인가 |
| **Utility** | Lombok | latest | 보일러플레이트 제거 |

### 2.2 Infrastructure

| 구분 | 기술 | 버전 | 용도 |
|---|---|---|---|
| **Database** | MySQL | 8.4 | 주 데이터 저장소 |
| **Cache** | Redis | 7.x | 캐싱, Refresh Token 저장, 재고 원자적 차감 |
| **Containerization** | Docker / Docker Compose | latest | 로컬 개발 환경 + 인프라 구성 |
| **CI/CD** | GitHub Actions | - | 테스트 → 빌드 → Docker 이미지 |

### 2.3 Testing

| 구분 | 기술 | 용도 |
|---|---|---|
| **Unit Test** | JUnit 5 (Jupiter) + AssertJ | 도메인 로직, Service 계층 |
| **Integration Test** | Testcontainers 1.20.x | MySQL, Redis 실환경 테스트 |
| **Concurrency Test** | ExecutorService + CountDownLatch | 동시성 정합성 검증 |
| **Load Test** | k6 | TPS, 응답시간, 에러율 측정 |

### 2.4 Documentation

| 구분 | 기술 |
|---|---|
| **API 문서** | Spring REST Docs 3.0 또는 Springdoc OpenAPI 2.x |
| **ERD** | dbdiagram.io 또는 IntelliJ Database Tools |

### 2.5 Spring Boot 3.5.10 선정 근거

Spring Boot 3.5는 2024년 11월 GA 릴리스되었으며, 2026년 2월 현재 3.5.10 패치까지 안정적으로 운영되고 있다. OSS 지원은 2026년 6월까지 보장된다.

**3.5.x를 선택한 이유 (vs 4.0.x)**:
- **안정성**: GA 이후 14개월, 패치 10회 — 프로덕션 레벨의 안정성 확보
- **커뮤니티 레퍼런스**: 블로그, 강의, Stack Overflow 대부분이 3.x 기반
- **트러블슈팅 용이성**: 1인 개발에서 breaking changes로 인한 디버깅 시간은 치명적
- **실무 정합성**: 2026년 2월 기준, 대부분의 한국 IT 기업이 Boot 3.x를 운영 중

**주요 기술 베이스라인**:
- **Spring Framework 6.2**: 안정적인 MVC, WebFlux, AOP 지원
- **Jakarta EE 10**: `jakarta.*` 네임스페이스 사용 (javax → jakarta 마이그레이션 완료)
- **Hibernate 6.6**: JPA 3.1 스펙 준수, 성능 최적화
- **Jackson 2.18.x**: 성숙한 JSON 직렬화/역직렬화, 풍부한 커뮤니티 지원
- **Virtual Threads 지원**: `spring.threads.virtual.enabled=true` 설정으로 활성화 가능 (Java 21)
- **Testcontainers 통합**: `@ServiceConnection`으로 간편한 테스트 인프라 설정
- **Observability 강화**: Micrometer + OpenTelemetry 자동 설정

---

## 3. 도메인 설계 (Domain Design)

### 3.1 핵심 도메인

```
┌─────────┐       ┌──────────────┐       ┌───────────┐
│  User   │       │   Product    │       │  TimeDeal │
│─────────│       │──────────────│       │───────────│
│ email   │       │ name         │◄──────│ productId │
│ password│       │ description  │       │ dealPrice │
│ nickname│       │ originalPrice│       │ dealStock │
│ role    │       │ stock        │       │ remaining │
│         │       │ category     │       │ startTime │
│         │       │              │       │ endTime   │
│         │       │              │       │ status    │
│         │       │              │       │ version   │
└────┬────┘       └──────────────┘       └─────┬─────┘
     │                                         │
     │            ┌──────────────┐              │
     └───────────►│    Order     │◄─────────────┘
                  │──────────────│
                  │ userId       │
                  │ timeDealId   │
                  │ quantity     │
                  │ totalPrice   │
                  │ status       │
                  │ createdAt    │
                  └──────────────┘
```

### 3.2 ERD

#### users

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 식별자 |
| password | VARCHAR(255) | NOT NULL | BCrypt 암호화 |
| nickname | VARCHAR(50) | NOT NULL | |
| role | ENUM('USER','ADMIN') | NOT NULL, DEFAULT 'USER' | |
| created_at | DATETIME(6) | NOT NULL | |
| deleted_at | DATETIME(6) | NULLABLE | Soft Delete |

#### products

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(255) | NOT NULL | |
| description | TEXT | | |
| original_price | INT | NOT NULL | 원가 (원) |
| stock | INT | NOT NULL, DEFAULT 0 | 일반 재고 |
| category | VARCHAR(100) | NOT NULL | |
| created_at | DATETIME(6) | NOT NULL | |
| updated_at | DATETIME(6) | NOT NULL | |

#### time_deals

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| product_id | BIGINT | FK → products(id), NOT NULL | |
| deal_price | INT | NOT NULL | 할인가 (원) |
| deal_stock | INT | NOT NULL | 타임딜 총 수량 |
| remaining_stock | INT | NOT NULL | 잔여 수량 |
| start_time | DATETIME(6) | NOT NULL | 딜 시작 시각 |
| end_time | DATETIME(6) | NOT NULL | 딜 종료 시각 |
| status | ENUM('UPCOMING','OPEN','CLOSED') | NOT NULL, DEFAULT 'UPCOMING' | |
| version | BIGINT | NOT NULL, DEFAULT 0 | 낙관적 락 (@Version) |
| created_at | DATETIME(6) | NOT NULL | |

#### orders

| 컬럼 | 타입 | 제약조건 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK → users(id), NOT NULL | |
| time_deal_id | BIGINT | FK → time_deals(id), NOT NULL | |
| quantity | INT | NOT NULL, DEFAULT 1 | |
| total_price | INT | NOT NULL | 결제 금액 |
| status | ENUM('CREATED','PAID','CANCELLED') | NOT NULL, DEFAULT 'CREATED' | |
| created_at | DATETIME(6) | NOT NULL | |
| updated_at | DATETIME(6) | NOT NULL | |

### 3.3 인덱스 설계

```sql
-- time_deals: 상태별 조회 + 시간순 정렬 (타임딜 목록 API)
CREATE INDEX idx_time_deals_status_start ON time_deals (status, start_time);

-- orders: 유저별 주문 내역 조회
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);

-- orders: 타임딜별 주문 집계
CREATE INDEX idx_orders_time_deal_status ON orders (time_deal_id, status);

-- products: 카테고리별 상품 목록
CREATE INDEX idx_products_category ON products (category, created_at DESC);
```

---

## 4. API 명세 (API Specification)

### 4.1 인증 (Auth)

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 | - |
| POST | `/api/v1/auth/login` | 로그인 → Access + Refresh Token 발급 | - |
| POST | `/api/v1/auth/logout` | 로그아웃 → Refresh Token 블랙리스트 | Bearer |
| POST | `/api/v1/auth/reissue` | Access Token 재발급 | Refresh |

### 4.2 회원 (User)

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/v1/users/me` | 내 정보 조회 | Bearer |
| PATCH | `/api/v1/users/me` | 내 정보 수정 (닉네임, 비밀번호) | Bearer |
| DELETE | `/api/v1/users/me` | 회원 탈퇴 (Soft Delete) | Bearer |

### 4.3 상품 (Product)

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/v1/admin/products` | 상품 등록 | ADMIN |
| PATCH | `/api/v1/admin/products/{id}` | 상품 수정 | ADMIN |
| DELETE | `/api/v1/admin/products/{id}` | 상품 삭제 | ADMIN |
| GET | `/api/v1/products` | 상품 목록 조회 (검색, 카테고리 필터, 커서 페이지네이션) | - |
| GET | `/api/v1/products/{id}` | 상품 상세 조회 | - |

### 4.4 타임딜 (TimeDeal)

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/v1/admin/time-deals` | 타임딜 생성 | ADMIN |
| PATCH | `/api/v1/admin/time-deals/{id}` | 타임딜 수정 (UPCOMING 상태만) | ADMIN |
| DELETE | `/api/v1/admin/time-deals/{id}` | 타임딜 삭제 (UPCOMING 상태만) | ADMIN |
| GET | `/api/v1/time-deals` | 타임딜 목록 조회 (상태 필터, 페이지네이션) | - |
| GET | `/api/v1/time-deals/{id}` | 타임딜 상세 조회 (잔여수량 포함) | - |

### 4.5 주문 (Order) — ⚡ 동시성 제어 핵심

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| **POST** | **`/api/v1/orders`** | **주문 생성 + 재고 차감** ⚡ | **Bearer** |
| POST | `/api/v1/orders/{id}/pay` | 결제 처리 | Bearer |
| POST | `/api/v1/orders/{id}/cancel` | 주문 취소 + 재고 복구 | Bearer |
| GET | `/api/v1/orders` | 내 주문 목록 (커서 페이지네이션) | Bearer |
| GET | `/api/v1/orders/{id}` | 주문 상세 | Bearer |

### 4.6 핵심 API 요청/응답 상세

프로젝트의 핵심인 주문 생성 API와 타임딜 조회 API의 상세 예시.

#### POST /api/v1/orders — 주문 생성 ⚡

```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "timeDealId": 1,
  "quantity": 1
}
```

**Response — 201 Created**
```json
{
  "status": "SUCCESS",
  "data": {
    "orderId": 1042,
    "timeDealId": 1,
    "productName": "나이키 에어맥스 90",
    "quantity": 1,
    "totalPrice": 89000,
    "status": "CREATED",
    "createdAt": "2026-03-15T14:30:00"
  }
}
```

**Response — 409 Conflict (재고 소진)**
```json
{
  "status": "ERROR",
  "data": null,
  "message": "타임딜 재고가 소진되었습니다.",
  "errorCode": "DEAL_STOCK_EXHAUSTED"
}
```

**Response — 409 Conflict (중복 주문)**
```json
{
  "status": "ERROR",
  "data": null,
  "message": "이미 해당 타임딜에 주문이 존재합니다.",
  "errorCode": "DUPLICATE_ORDER"
}
```

#### GET /api/v1/time-deals — 타임딜 목록 조회

```
GET /api/v1/time-deals?status=OPEN&cursor=eyJpZCI6NTB9&size=10
```

**Response — 200 OK**
```json
{
  "status": "SUCCESS",
  "data": {
    "content": [
      {
        "timeDealId": 49,
        "productName": "나이키 에어맥스 90",
        "originalPrice": 169000,
        "dealPrice": 89000,
        "discountRate": 47,
        "remainingStock": 23,
        "dealStock": 100,
        "startTime": "2026-03-15T14:00:00",
        "endTime": "2026-03-15T16:00:00",
        "status": "OPEN"
      }
    ],
    "nextCursor": "eyJpZCI6NDB9",
    "hasNext": true,
    "size": 10
  }
}
```

#### POST /api/v1/auth/login — 로그인

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

**Response — 200 OK**
```json
{
  "status": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

### 4.7 에러 코드 정의

| HTTP Status | Error Code | 설명 |
|---|---|---|
| 400 | `INVALID_INPUT` | 요청 파라미터 유효성 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증 실패 (토큰 누락/만료) |
| 403 | `FORBIDDEN` | 권한 부족 |
| 404 | `USER_NOT_FOUND` | 사용자 없음 |
| 404 | `PRODUCT_NOT_FOUND` | 상품 없음 |
| 404 | `DEAL_NOT_FOUND` | 타임딜 없음 |
| 404 | `ORDER_NOT_FOUND` | 주문 없음 |
| 409 | `DEAL_NOT_OPEN` | 타임딜이 OPEN 상태가 아님 |
| 409 | `DEAL_STOCK_EXHAUSTED` | 타임딜 재고 소진 |
| 409 | `DUPLICATE_ORDER` | 동일 타임딜 중복 주문 |
| 409 | `ORDER_ALREADY_PAID` | 이미 결제 완료된 주문 |
| 409 | `ORDER_ALREADY_CANCELLED` | 이미 취소된 주문 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

---

## 5. 핵심 기능 상세 (Feature Details)

### 5.1 회원 및 인증/인가

#### 5.1.1 회원가입

- 이메일 형식 검증 + 중복 확인
- 비밀번호: 8자 이상, 영문 + 숫자 + 특수문자 포함 → BCrypt 해싱
- 닉네임: 2~20자, 중복 불가

#### 5.1.2 JWT 인증 흐름

```
[로그인 요청]
    │
    ▼
[이메일/비밀번호 검증]
    │
    ▼
[Access Token 발급 (30분)] + [Refresh Token 발급 (14일, Redis 저장)]
    │
    ▼
[클라이언트: Authorization: Bearer {accessToken}]
    │
    ▼
[Access Token 만료 시]
    │
    ▼
[POST /api/v1/auth/reissue + Refresh Token]
    │
    ▼
[새 Access Token 발급 + Refresh Token Rotation]
```

- **Access Token**: 30분 만료, 응답 Header 또는 Body로 전달
- **Refresh Token**: 14일 만료, Redis에 `refresh:{userId}` 키로 저장
- **로그아웃**: Refresh Token 삭제 + Access Token을 Redis 블랙리스트에 TTL과 함께 등록
- **Refresh Token Rotation**: 재발급 시 기존 Refresh Token 무효화, 새 토큰 발급

#### 5.1.3 인가 (Authorization)

| Role | 권한 |
|---|---|
| USER | 상품/타임딜 조회, 주문 생성/조회/취소, 내 정보 관리 |
| ADMIN | USER 권한 전체 + 상품 CRUD + 타임딜 CRUD |

- Spring Security 6.5 `SecurityFilterChain` 기반 설정
- `@PreAuthorize("hasRole('ADMIN')")` 또는 커스텀 `AuthorizationManager`
- 본인 리소스 접근 검증: 주문 조회/취소 시 `order.userId == currentUser.id` 체크

### 5.2 타임딜 상태 관리

```
[UPCOMING] ──── startTime 도달 ────► [OPEN] ──── endTime 도달 OR 재고 소진 ────► [CLOSED]
```

- **Spring Scheduler** (`@Scheduled`)로 1분 주기 상태 전이 체크
- `UPCOMING → OPEN`: `start_time <= NOW() AND status = 'UPCOMING'`
- `OPEN → CLOSED`: `end_time <= NOW() AND status = 'OPEN'` 또는 `remaining_stock = 0`
- 상태 전이는 벌크 UPDATE 쿼리로 처리 (개별 엔티티 로드 불필요)

### 5.3 동시성 제어 — 단계별 구현 및 비교 ⚡

본 프로젝트의 핵심 어필 포인트. 동일한 시나리오(100개 재고에 1,000건 동시 주문)에 대해 4가지 전략을 순차적으로 구현하고, 각 단계의 성능과 정합성을 비교 분석한다.

#### Phase 1: synchronized

```java
public synchronized OrderResponse createOrder(OrderRequest request) {
    // 재고 확인 → 차감 → 주문 생성
}
```

- **장점**: 구현 간단, 단일 인스턴스에서 정합성 보장
- **한계**: 멀티 인스턴스 환경 불가, 처리량 병목

#### Phase 2: DB 비관적 락 (Pessimistic Lock)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT td FROM TimeDeal td WHERE td.id = :id")
Optional<TimeDeal> findByIdForUpdate(@Param("id") Long id);
```

- **장점**: 멀티 인스턴스 환경에서 정합성 보장
- **한계**: DB 커넥션 점유 시간 증가, 동시 요청 많을수록 대기 시간 증가
- **주의**: HikariCP 커넥션 풀 크기에 직접 영향 — 사람인 기술블로그의 HikariCP 장애 회고처럼, 커넥션 점유 시간이 길어지면 풀 고갈 → 연쇄 장애로 이어질 수 있음

#### Phase 3: DB 낙관적 락 (Optimistic Lock)

```java
@Version
private Long version;
```

- **장점**: 락 대기 없음, 읽기 성능 우수
- **한계**: 충돌 시 `OptimisticLockingFailureException` → 재시도 로직 필요, 경합이 심하면 성능 저하

#### Phase 4: Redis 원자적 연산

```lua
-- Lua Script: 원자적 재고 차감
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil or stock <= 0 then
    return -1
end
return redis.call('DECR', KEYS[1])
```

- **장점**: DB 부하 제거, 원자적 연산으로 정합성 보장, 높은 TPS
- **한계**: Redis 장애 시 대응 필요, DB와 Redis 간 데이터 정합성 관리
- **설계 근거**: 우아한형제들 빼빼로데이 이벤트에서 Redis Replication Read 시 카운터 불일치로 초과 발급 발생 → 본 프로젝트는 Master 노드 단일 Lua Script으로 원자성 확보. 여기어때 기술블로그의 Redis INCR + Kafka 비동기 DB Write 패턴도 참고하여, 재고 차감(Redis)과 주문 저장(DB)을 분리

#### 비교 측정 항목

| 측정 항목 | 도구 |
|---|---|
| 정합성 (초과 판매 건수) | ExecutorService + CountDownLatch (100~1,000 동시 스레드) |
| TPS (초당 처리량) | k6 |
| 평균 응답 시간 | k6 |
| p95 / p99 응답 시간 | k6 |
| 에러율 | k6 |
| DB 커넥션 사용량 | HikariCP 메트릭 |

#### 기대 결과 (리포트 템플릿)

```
┌──────────────────────────────────────────────────────────────┐
│              동시성 제어 전략 비교 리포트                       │
│                                                              │
│  시나리오: 100개 재고, 1,000건 동시 주문                       │
│                                                              │
│  전략          │ 초과판매 │ TPS    │ p95(ms) │ 커넥션 사용량   │
│  ─────────────│─────────│────────│─────────│───────────────  │
│  synchronized │ 0건     │ ???    │ ???     │ 1              │
│  비관적 락     │ 0건     │ ???    │ ???     │ pool 최대치     │
│  낙관적 락     │ 0건     │ ???    │ ???     │ 유동적          │
│  Redis DECR   │ 0건     │ ???    │ ???     │ 0 (Redis)      │
│                                                              │
│  ✅ 결론: Redis DECR이 TPS ???배 우수, DB 부하 제거            │
└──────────────────────────────────────────────────────────────┘
```

### 5.4 캐싱 전략

쿠팡 기술블로그에서 제시한 "데이터 변경 빈도에 따른 캐싱 전략 분리" 원칙을 참고한다. 일반 데이터는 Read-through(TTL) 캐시로, 재고처럼 초 단위 변경이 필요한 데이터는 실시간 캐시로 분리한다.

| 대상 | 캐시 키 패턴 | 전략 | TTL | 변경 빈도 |
|---|---|---|---|---|
| 타임딜 목록 (OPEN) | `cache:deals:open` | Cache Aside | 30초 | 분 단위 |
| 타임딜 상세 | `cache:deal:{id}` | Cache Aside | 60초 | 분 단위 |
| 상품 상세 | `cache:product:{id}` | Cache Aside | 5분 | 일 단위 |
| 타임딜 잔여수량 | `deal:stock:{dealId}` | Redis 직접 조회 (Write-Through) | 없음 (실시간) | 초 단위 |

**캐시 무효화 전략**:
- 타임딜 상태 변경 시 관련 캐시 명시적 삭제
- 상품 수정 시 해당 상품 캐시 삭제
- TTL 기반 자동 만료를 1차 방어선으로 사용

**Redis 직렬화 전략**:
- 캐시 값: `Jackson2JsonRedisSerializer` — JSON 가독성 + 디버깅 용이
- Refresh Token / 블랙리스트: `StringRedisSerializer` — 단순 문자열, 오버헤드 최소화

### 5.5 비동기 이벤트 처리

우아한형제들 배민스토어의 이벤트 기반 아키텍처를 참고하되, 현재 스코프에서는 Spring 내부 이벤트로 구현하고 향후 Kafka 전환이 가능하도록 인터페이스를 추상화한다.

```
[주문 생성 완료]
    │
    ▼
[ApplicationEvent 발행: OrderCreatedEvent]
    │
    ├──► [@TransactionalEventListener] 주문 이력 저장
    └──► [@Async] 알림 처리 (로그 기록, 향후 확장 포인트)
```

- Spring `ApplicationEventPublisher`를 통한 도메인 이벤트 발행
- `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 트랜잭션 커밋 후 처리
- `@Async` + `CompletableFuture`로 비동기 처리
- **확장 포인트**: `EventPublisher` 인터페이스를 정의하고, 현재는 `SpringEventPublisher` 구현체 사용. 향후 `KafkaEventPublisher`로 교체 가능한 구조

### 5.6 DB 최적화

| 최적화 항목 | 적용 대상 | 검증 방법 |
|---|---|---|
| 인덱스 설계 | 전체 테이블 | `EXPLAIN ANALYZE` 실행 계획 분석 |
| N+1 해결 | 타임딜 목록 (Product 조인) | `@EntityGraph` 또는 `fetch join` |
| offset → 커서 페이지네이션 | 주문 목록, 상품 목록 | 대량 데이터 기준 응답시간 비교 |
| 벌크 UPDATE | 타임딜 상태 전이 | `@Modifying @Query` JPQL |

---

## 6. 비기능 요구사항 (Non-Functional Requirements)

### 6.1 보안

| 항목 | 구현 |
|---|---|
| 비밀번호 암호화 | BCrypt (strength 10) |
| JWT 서명 | HMAC-SHA256 또는 RSA |
| Refresh Token 저장 | Redis (서버 사이드, HttpOnly 불필요) |
| CORS | 허용 Origin 화이트리스트 |
| SQL Injection | JPA Parameterized Query (기본 방어) |
| XSS | Jackson 자동 이스케이프 + 입력 검증 |
| Rate Limiting | 고려사항으로 문서화 (구현은 선택) |

### 6.2 로깅 및 모니터링

토스 기술블로그에서 강조하는 "성능 개선의 시작과 끝은 모니터링"이라는 원칙을 따른다.

**로깅 전략**:
- **Logback** 기반 구조화된 로깅
- 요청/응답 로깅: `HandlerInterceptor` 또는 `Filter`
- 로그 레벨 전략: `ERROR` (장애), `WARN` (비즈니스 예외), `INFO` (요청/응답), `DEBUG` (개발)
- 민감 정보 마스킹: 비밀번호, 토큰 등

**모니터링 (Actuator 기반)**:
- `spring.boot.actuator` 엔드포인트 활성화 (`health`, `metrics`, `info`)
- HikariCP 메트릭 노출: `hikaricp.connections.active`, `hikaricp.connections.idle`, `hikaricp.connections.pending`
- 커넥션 누수 감지: `spring.datasource.hikari.leak-detection-threshold=30000` (30초)
- Redis 커넥션 상태 모니터링

### 6.3 HikariCP 커넥션 풀 설정

사람인 기술블로그의 HikariCP 장애 사례를 참고하여, 동시성 테스트에서의 커넥션 풀 동작을 명확히 관찰할 수 있도록 설정한다.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10          # 기본값. 동시성 테스트 시 의도적으로 조절하여 비교
      minimum-idle: 5
      connection-timeout: 3000       # 3초. 커넥션 획득 실패 시 빠르게 에러 반환
      max-lifetime: 1800000          # 30분
      idle-timeout: 600000           # 10분
      leak-detection-threshold: 30000 # 30초. 커넥션 누수 의심 시 WARN 로깅
```

**동시성 테스트별 커넥션 풀 관찰 포인트**:
- **비관적 락**: 커넥션 점유 시간 ↑ → `pending` 카운트 ↑ → TPS ↓ 상관관계 측정
- **낙관적 락**: 재시도 시 커넥션 재획득 → 전체 커넥션 사용 패턴 분석
- **Redis DECR**: DB 커넥션 사용량 ↓ → 주문 INSERT만 사용 → 커넥션 여유 확인

### 6.4 예외 처리

- `@RestControllerAdvice` 글로벌 예외 핸들러
- 비즈니스 예외: `CustomException` 계층 구조 (ErrorCode enum 연동)
- 예외 응답: 일관된 JSON 포맷 (`status`, `message`, `errorCode`)
- Validation 예외: `@Valid` + `MethodArgumentNotValidException` 핸들링

### 6.5 테스트 전략

| 계층 | 대상 | 도구 | 목표 |
|---|---|---|---|
| Unit | 도메인 모델, Service 로직 | JUnit 5 + Mockito | 비즈니스 로직 정확성 |
| Integration | Repository, API 엔드포인트 | Testcontainers 1.20.x + MockMvc | DB/Redis 연동 검증 |
| Concurrency | 재고 차감 동시 요청 | ExecutorService + CountDownLatch | 정합성 보장 |
| Load | 주문 API | k6 | TPS, 응답시간, 에러율 |

---

## 7. 코딩 가이드라인 (Coding Guidelines)

본 프로젝트는 단순히 "동작하는 코드"가 아니라, **"읽기 좋고 유지보수 가능한 코드"**를 작성하는 것을 목표로 한다. 우아한테크코스(이하 우테코)의 프리코스 프로그래밍 요구사항과 객체지향 생활체조 원칙, 클린 코드 원칙, RESTful API 설계 원칙을 준수한다.

### 7.1 객체지향 설계 원칙 (OOP)

#### SOLID 원칙 적용

| 원칙 | 적용 포인트 | 프로젝트 내 구체 사례 |
|---|---|---|
| **SRP** (단일 책임) | 하나의 클래스는 하나의 책임만 가진다 | `OrderService`는 주문 생성만 담당, 재고 차감은 `StockService`로 분리 |
| **OCP** (개방-폐쇄) | 확장에 열리고 변경에 닫힌 구조 | `StockService` 인터페이스 + 4개 구현체 — 새 전략 추가 시 기존 코드 수정 없음 |
| **LSP** (리스코프 치환) | 하위 타입은 상위 타입을 대체 가능 | `RedisStockService`든 `PessimisticStockService`든 `StockService`로 동일하게 동작 |
| **ISP** (인터페이스 분리) | 클라이언트에 필요한 인터페이스만 노출 | `EventPublisher` 인터페이스를 발행 전용으로 설계, 구독 로직은 별도 |
| **DIP** (의존성 역전) | 구체 클래스가 아닌 추상화에 의존 | `OrderService` → `StockService`(인터페이스)에 의존, 구현체는 런타임 주입 |

#### 객체지향 생활체조 9가지 원칙 (ThoughtWorks Anthology)

우테코에서 강조하는 "의식적인 연습"으로, 모든 코드 작성 시 아래 원칙을 체크리스트로 활용한다.

| # | 원칙 | 준수 방법 |
|---|---|---|
| 1 | **한 메서드에 오직 한 단계의 들여쓰기** | indent depth 최대 2. 중첩이 깊어지면 메서드 분리 |
| 2 | **else 예약어를 쓰지 않는다** | Early Return 패턴, Guard Clause 활용. 다형성으로 분기 제거 |
| 3 | **모든 원시값과 문자열을 포장한다** | `Money`, `Stock`, `DealPrice` 등 도메인 의미를 가진 값은 VO로 포장 검토 |
| 4 | **한 줄에 점을 하나만 찍는다** | 디미터 법칙(Law of Demeter) 준수. `order.getUser().getName()` → `order.getUserName()` |
| 5 | **줄여 쓰지 않는다 (축약 금지)** | `cmd` → `command`, `qty` → `quantity`. 의도가 드러나는 이름 사용 |
| 6 | **모든 엔티티를 작게 유지한다** | 클래스 200줄 이하 목표. 비대해지면 책임 분리 신호 |
| 7 | **3개 이상의 인스턴스 변수를 가진 클래스 지양** | 인스턴스 변수가 많으면 응집도 ↓. 관련 변수를 별도 객체로 추출 |
| 8 | **일급 컬렉션을 쓴다** | 컬렉션을 감싸는 래퍼 클래스 활용 검토 (예: `Orders`, `TimeDealList`) |
| 9 | **Getter/Setter를 쓰지 않는다** | 객체에 메시지를 보내는 방식. `deal.getRemainingStock() > 0` → `deal.isAvailable()` |

> **현실적 적용 기준**: 9가지 원칙을 100% 엄격하게 적용하면 오히려 과도한 추상화가 될 수 있다. JPA Entity의 Getter, DTO의 직렬화용 Getter 등 프레임워크 요구사항은 예외로 허용한다. 핵심은 **비즈니스 로직이 담긴 도메인 객체에서 이 원칙들을 의식적으로 실천**하는 것이다.

#### 설계 패턴 적용

| 패턴 | 적용 위치 | 설명 |
|---|---|---|
| **전략 패턴 (Strategy)** | `StockService` + 4개 구현체 | 동시성 제어 전략을 런타임에 교체 가능 |
| **템플릿 메서드 / 팩토리** | `CustomException` 계층 | ErrorCode enum과 결합한 예외 생성 |
| **옵저버 패턴 (Observer)** | `ApplicationEvent` 기반 이벤트 | 주문 생성 → 이벤트 발행 → 리스너 처리 |
| **빌더 패턴 (Builder)** | DTO, 응답 객체 | `@Builder`로 가독성 높은 객체 생성 |

### 7.2 클린 코드 (Clean Code)

#### 네이밍 컨벤션

본 프로젝트는 **네이버 캠퍼스 핵데이 Java 코딩 컨벤션**을 기반으로 하며, 우테코 프리코스 요구사항을 준수한다.

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스 | `UpperCamelCase`, 명사/명사구 | `OrderService`, `TimeDealScheduler` |
| 메서드 | `lowerCamelCase`, 동사/동사구 | `createOrder()`, `validateStock()` |
| 변수 | `lowerCamelCase`, 의미 있는 이름 | `remainingStock`, `dealPrice` |
| 상수 | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT`, `CACHE_TTL_SECONDS` |
| 패키지 | `lowercase`, 단수형 | `com.snapstock.domain.order` |
| 테스트 메서드 | `메서드명_조건_기대결과` (한글 허용) | `createOrder_재고부족_예외발생()` |

#### 메서드 작성 원칙

```
✅ 한 메서드는 한 가지 일만 한다 (SRP)
✅ 메서드 길이 15줄 이하 권장 (우테코 기준)
✅ 인덴트 depth 최대 2 (우테코 프리코스 요구사항)
✅ else 사용 금지 → Early Return 패턴
✅ 매개변수 3개 이하 (3개 초과 시 객체로 포장)
✅ public 메서드 → private 메서드 순서 (신문 기사처럼 읽히도록)
```

#### 예시: Early Return 적용

```java
// ❌ BAD — else 중첩, 가독성 저하
public void createOrder(OrderRequest request) {
    if (deal.getStatus() == DealStatus.OPEN) {
        if (deal.getRemainingStock() > 0) {
            // 주문 생성 로직
        } else {
            throw new CustomException(ErrorCode.DEAL_STOCK_EXHAUSTED);
        }
    } else {
        throw new CustomException(ErrorCode.DEAL_NOT_OPEN);
    }
}

// ✅ GOOD — Early Return, Guard Clause, 메시지 전달
public void createOrder(OrderRequest request) {
    TimeDeal deal = findDealOrThrow(request.getTimeDealId());
    deal.validateOpen();          // 객체에게 검증 책임 위임
    deal.validateStockAvailable(); // 객체가 스스로 상태 판단

    // 주문 생성 로직 (핵심 로직만 남음)
}
```

#### 주석 원칙

```
✅ 코드 자체가 문서가 되도록 작성 (자기 문서화 코드)
✅ "왜(Why)"에 대한 주석만 작성 — "무엇(What)"은 코드로 표현
✅ TODO/FIXME는 이슈 번호와 함께 기록
❌ 주석으로 코드를 설명하지 않는다 (메서드명이 설명)
❌ 주석 처리된 코드를 남기지 않는다 (Git이 이력 관리)
```

### 7.3 HTTP API 설계 원칙

#### RESTful URI 설계 규칙

| 규칙 | 올바른 예 | 잘못된 예 |
|---|---|---|
| URI에 동사 사용 금지 — HTTP 메서드가 행위를 표현 | `POST /api/v1/orders` | `POST /api/v1/createOrder` |
| 리소스명은 복수형 명사 | `/api/v1/products` | `/api/v1/product` |
| 계층 관계는 `/`로 표현 | `/api/v1/users/{id}/orders` | `/api/v1/getUserOrders` |
| URI 경로에는 소문자 + 하이픈(`-`) | `/api/v1/time-deals` | `/api/v1/timeDeals` |
| 파일 확장자 미포함 | `/api/v1/products/1` | `/api/v1/products/1.json` |
| 마지막 `/` 미포함 | `/api/v1/orders` | `/api/v1/orders/` |
| URI depth 3단계 이하 권장 | `/api/v1/orders/{id}` | `/api/v1/users/{uid}/deals/{did}/orders/{oid}/items` |

#### HTTP 메서드 사용 원칙

| 메서드 | 용도 | 멱등성 | 안전성 | 성공 응답 코드 |
|---|---|---|---|---|
| `GET` | 리소스 조회 | ✅ | ✅ | `200 OK` |
| `POST` | 리소스 생성 | ❌ | ❌ | `201 Created` (+ `Location` 헤더) |
| `PATCH` | 리소스 부분 수정 | ❌ | ❌ | `200 OK` |
| `DELETE` | 리소스 삭제 | ✅ | ❌ | `204 No Content` |

#### HTTP 상태 코드 사용 원칙

모든 응답은 의미에 맞는 HTTP 상태 코드를 반환한다. 모든 요청을 `200 OK`로 처리하고 body에 커스텀 코드를 넣는 안티 패턴을 지양한다.

| 코드 | 의미 | 사용 시점 |
|---|---|---|
| `200` | OK | 조회, 수정 성공 |
| `201` | Created | 리소스 생성 성공 (주문 생성, 회원가입) |
| `204` | No Content | 삭제 성공 (응답 body 없음) |
| `400` | Bad Request | 요청 파라미터 유효성 실패 |
| `401` | Unauthorized | 인증 실패 (토큰 누락/만료) |
| `403` | Forbidden | 인가 실패 (권한 부족) |
| `404` | Not Found | 리소스 없음 |
| `409` | Conflict | 비즈니스 규칙 위반 (재고 소진, 중복 주문, 상태 불일치) |
| `500` | Internal Server Error | 서버 내부 오류 |

#### 응답 형식 통일

성공과 실패 모두 동일한 envelope 구조를 사용한다. 프론트엔드에서 일관된 파싱이 가능하도록 한다.

**성공 응답**:
```json
{
  "status": "SUCCESS",
  "data": { ... },
  "message": null,
  "errorCode": null
}
```

**실패 응답**:
```json
{
  "status": "ERROR",
  "data": null,
  "message": "타임딜 재고가 소진되었습니다.",
  "errorCode": "DEAL_STOCK_EXHAUSTED"
}
```

**Validation 실패 응답**:
```json
{
  "status": "ERROR",
  "data": null,
  "message": "입력값이 올바르지 않습니다.",
  "errorCode": "INVALID_INPUT",
  "fieldErrors": [
    {
      "field": "email",
      "value": "invalid",
      "reason": "올바른 이메일 형식이 아닙니다."
    }
  ]
}
```

#### API 버전 관리

- URI Path 방식 채택: `/api/v1/...`
- 이유: 명시적이고 직관적, 대부분의 국내 서비스에서 채택하는 방식
- 향후 breaking change 발생 시 `/api/v2/...`로 분리

### 7.4 코드 품질 체크리스트

매 PR(Pull Request) 전 아래 체크리스트를 확인한다. 우테코 코드 리뷰 기준을 참고하여 작성하였다.

```
[ ] 인덴트 depth가 3 이상인 메서드가 없는가?
[ ] else 예약어를 사용한 곳이 없는가?
[ ] 메서드가 한 가지 일만 하는가? (15줄 이하)
[ ] 매개변수가 3개를 초과하는 메서드가 없는가?
[ ] 도메인 객체에 불필요한 Getter가 없는가? (객체에 메시지를 보내고 있는가?)
[ ] 하드코딩된 값이 없는가? (상수 또는 설정으로 분리)
[ ] 테스트 코드가 작성되었는가?
[ ] 변수명/메서드명만으로 의도를 파악할 수 있는가?
[ ] HTTP 메서드와 상태 코드가 RESTful 원칙에 맞는가?
[ ] 커밋 메시지가 Conventional Commits 형식인가?
```

---

## 8. 프로젝트 구조 (Project Structure)

### 7.1 패키지 구조

도메인형(기능형) 패키지 구조를 채택한다. 레이어드 구조(`controller/`, `service/`, `repository/` 최상위 분리)는 도메인이 늘어날수록 파일 탐색이 어렵고, 도메인 간 경계가 모호해진다. 도메인형 구조는 각 도메인이 독립적인 패키지를 가지므로 응집도가 높고, 향후 모듈 분리나 MSA 전환 시에도 유리하다.

```
com.snapstock
├── global/                     # 공통 설정 및 인프라
│   ├── config/                 # SecurityConfig, RedisConfig, AsyncConfig
│   ├── error/                  # GlobalExceptionHandler, ErrorCode, CustomException
│   ├── auth/                   # JwtTokenProvider, JwtAuthenticationFilter
│   ├── common/                 # BaseEntity, ApiResponse, CursorPageResponse
│   └── util/                   # 공용 유틸리티
├── domain/
│   ├── user/                   # 회원 도메인
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/                # request/, response/
│   ├── product/                # 상품 도메인
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── timedeal/               # 타임딜 도메인
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── scheduler/         # TimeDealScheduler (상태 전이)
│   └── order/                  # 주문 도메인 ⚡
│       ├── controller/
│       ├── service/
│       │   ├── OrderService.java
│       │   └── stock/          # StockService 인터페이스 + 4가지 구현체
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       └── event/              # OrderCreatedEvent, OrderEventListener
└── SnapStockApplication.java
```

**설계 원칙**:
- 각 도메인 패키지는 `controller → service → repository` 단방향 의존
- 도메인 간 의존은 Service 계층에서만 허용 (예: `OrderService` → `TimeDealService`)
- `global/`은 모든 도메인에서 참조 가능, 역방향 참조 금지
- 동시성 제어 4단계는 `order/service/stock/` 아래에 `StockService` 인터페이스와 구현체(`SyncStockService`, `PessimisticStockService`, `OptimisticStockService`, `RedisStockService`)로 전략 패턴 적용

### 7.2 Git 브랜치 전략

```
main ─────────────────────────────────────────────── 배포 가능 상태
  │
  └── develop ────────────────────────────────────── 개발 통합 브랜치
        │
        ├── feat/auth ────── 회원/인증/인가
        ├── feat/product ─── 상품 CRUD
        ├── feat/timedeal ── 타임딜 CRUD + 스케줄러
        ├── feat/order ───── 주문 + 동시성 제어
        ├── feat/cache ───── Redis 캐싱
        ├── feat/async ───── 비동기 이벤트
        ├── feat/infra ───── Docker + CI/CD
        └── fix/xxx ──────── 버그 수정
```

**브랜치 규칙**:
- `main`: 항상 빌드·테스트 통과 상태 유지. develop에서 PR 머지로만 반영
- `develop`: 기능 브랜치 통합. CI 자동 테스트
- `feat/*`: 기능 단위 브랜치. 작업 완료 후 develop으로 PR
- `fix/*`: 버그 수정 브랜치

**커밋 컨벤션 (Conventional Commits)**:

```
<type>: <subject>

[body]
```

| Type | 용도 | 예시 |
|---|---|---|
| `feat` | 새 기능 | `feat: 주문 생성 API 구현` |
| `fix` | 버그 수정 | `fix: 동시성 테스트 재고 초과 판매 수정` |
| `refactor` | 리팩토링 | `refactor: StockService 전략 패턴 적용` |
| `test` | 테스트 | `test: 비관적 락 동시성 테스트 추가` |
| `docs` | 문서 | `docs: README 동시성 비교 표 추가` |
| `chore` | 빌드/설정 | `chore: Docker Compose Redis 추가` |
| `perf` | 성능 개선 | `perf: 타임딜 목록 캐싱 적용` |

### 7.3 README 구조 가이드

면접관이 GitHub에서 가장 먼저 보는 것은 README이다. 아래 구조를 기준으로 프로젝트 완성 시 작성한다.

```markdown
# SnapStock ⚡
> 타임딜 커머스 플랫폼 — 동시성 제어와 대용량 처리 중심 백엔드 포트폴리오

## 프로젝트 소개
- 한 줄 요약 + 핵심 기술 목표

## 기술 스택
- 뱃지 이미지로 시각화 (Java 21, Spring Boot 3.5, MySQL 8.4, Redis 7)

## 아키텍처
- 시스템 구성도 이미지 (draw.io 또는 Mermaid)
- 패키지 구조 요약

## 핵심 기능
- 동시성 제어 4단계 비교 (표 + 그래프 이미지) ⭐
- 캐싱 전후 성능 비교
- 부하 테스트 결과 (k6 리포트 스크린샷)

## ERD
- dbdiagram.io 이미지

## API 명세
- Swagger UI 링크 또는 REST Docs 링크

## 실행 방법
- docker-compose up 원커맨드 구동

## 트러블슈팅
- 개발 중 겪은 문제 + 해결 과정 (2~3개)

## 회고
- 배운 점, 아쉬운 점, 개선 방향
```

**README 작성 원칙**:
- 동시성 비교 표와 성능 그래프는 반드시 포함 — 이것이 프로젝트의 핵심 어필 포인트
- 트러블슈팅 섹션은 면접 대비 최고의 자산. "이런 문제 → 이렇게 분석 → 이렇게 해결" 흐름
- 실행 방법은 `git clone → docker-compose up → 접속` 3줄 이내

---

## 9. 인프라 및 배포 (Infrastructure)

### 9.1 Docker Compose 구성

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on: [mysql, redis]
    environment:
      SPRING_PROFILES_ACTIVE: local
  mysql:
    image: mysql:8.4
    ports: ["3306:3306"]
    volumes: [mysql-data:/var/lib/mysql]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

### 9.2 CI/CD 파이프라인 (GitHub Actions)

```
[Push to main]
    │
    ├──► Checkout
    ├──► JDK 21 Setup
    ├──► Gradle Build + Test
    ├──► Docker Image Build
    └──► (선택) Docker Hub Push
```

### 9.3 프로파일 관리

| 프로파일 | 용도 | DB | Redis |
|---|---|---|---|
| `local` | 로컬 개발 | Docker MySQL | Docker Redis |
| `test` | 테스트 | Testcontainers | Testcontainers |

---

## 10. 개발 일정 (Timeline)

총 **9주** 소요 예상. 1인 개발 기준 현실적 일정.

| 주차 | Phase | 주요 산출물 | 완료 기준 |
|---|---|---|---|
| **1~2주** | 기본기 | 회원/인증/인가 API + 테스트 | 회원가입 → 로그인 → 토큰 재발급 → 로그아웃 전체 플로우 통합 테스트 통과 |
| **3~4주** | 핵심 CRUD | 상품/타임딜/주문 API + ERD 확정 | 모든 CRUD API 동작 + REST Docs 또는 Swagger 문서 생성 |
| **5~6주** | 동시성 제어 | 4단계 락 전략 구현 + 비교 리포트 | 동시성 테스트 통과 + 단계별 성능 비교 표 완성 |
| **7주** | 캐싱 + DB 최적화 | Redis 캐시 적용 + 쿼리 튜닝 | EXPLAIN 분석 기록 + 캐싱 전후 응답시간 비교 |
| **8주** | 비동기 + 테스트 | 이벤트 분리 + 부하 테스트 | k6 리포트 (TPS, p95, 에러율) 완성 |
| **9주** | 인프라 + 문서화 | Docker + CI/CD + README | docker-compose up 원커맨드 구동 + GitHub Actions 파이프라인 정상 동작 |

---

## 11. 리스크 및 의사결정 기록 (ADR)

### ADR-001: Spring Boot 3.5.10 채택

- **맥락**: 2026년 2월 현재 Spring Boot 4.0.2(GA 3개월)와 3.5.10(GA 14개월, 패치 10회)이 공존. 1인 포트폴리오 프로젝트에서 안정성과 개발 속도가 최우선.
- **결정**: Spring Boot 3.5.10(Spring Framework 6.2, Jakarta EE 10, Hibernate 6.6)을 채택.
- **근거**: 커뮤니티 레퍼런스 풍부, 트러블슈팅 용이, 실무(한국 IT 기업 대부분 3.x 운영)와의 정합성. 면접에서 중요한 것은 프레임워크 버전이 아니라 동시성 제어·성능 최적화 등 기술적 깊이.
- **리스크**: OSS 지원 2026년 6월 종료 예정. 프로젝트 완성 후 필요 시 4.0 마이그레이션 가능.

### ADR-002: 동시성 전략 — Redis DECR 최종 채택

- **맥락**: 타임딜 재고 차감은 가장 높은 동시성 부하가 예상되는 연산. 우아한형제들 빼빼로데이 이벤트에서 Redis Replication Read 시 카운터 불일치로 초과 발급이 발생한 사례가 있음.
- **결정**: 4가지 전략을 모두 구현하되, 최종 운영 전략은 Redis Lua Script 기반 원자적 차감으로 결정.
- **근거**: DB 부하 분리, 원자성 보장, 높은 TPS. Redis 장애 시에는 DB 비관적 락으로 폴백 가능하도록 설계. 여기어때의 Redis INCR + Kafka 비동기 DB Write 패턴을 참고하여, 재고 확인은 Redis에서 수행하고 주문 저장은 DB에 비동기로 처리하는 구조 적용.

### ADR-003: 커서 기반 페이지네이션

- **맥락**: offset 기반 페이지네이션은 데이터가 많아질수록 성능 저하.
- **결정**: 주문 목록, 상품 목록 등 주요 목록 API에 커서 기반 페이지네이션 적용.
- **구현**: `WHERE id < :cursor ORDER BY id DESC LIMIT :size` 패턴.

### ADR-004: Jackson 2.18.x 사용

- **맥락**: Spring Boot 3.5는 Jackson 2.18.x를 기본 사용하며, 커뮤니티에서 가장 널리 쓰이는 안정적인 JSON 라이브러리.
- **결정**: `com.fasterxml.jackson.annotation.*` 패키지 기반 어노테이션 사용. 커스텀 직렬화가 필요한 경우 Jackson 2.x API 기준으로 작성.

### ADR-005: 도메인형 패키지 구조 채택

- **맥락**: 레이어드 구조는 도메인이 늘어날수록 파일 탐색이 어렵고 도메인 간 경계가 모호. 우아한형제들을 비롯한 국내 IT 기업들이 MSA 환경에서 도메인 단위 시스템을 구현하는 추세.
- **결정**: `com.snapstock.domain.{user,product,timedeal,order}` 도메인형 구조 채택.
- **근거**: 높은 응집도, 도메인 간 명확한 경계, 향후 모듈 분리 시 유리. 동시성 제어 4단계 구현체는 전략 패턴으로 `StockService` 인터페이스 아래에 배치하여 교체 용이성 확보.

---

## 12. 확장 고려사항 (Future Considerations)

현재 스코프에 포함하지 않으나, 아키텍처 설계 시 확장 가능성을 고려하는 항목.

| 항목 | 설명 | 현재 대응 | 참고 사례 |
|---|---|---|---|
| 대기열 시스템 | Redis Sorted Set 기반 접속자 대기열 | 인터페이스 설계만 수행, 구현은 Phase 2 | 네이버 콘퍼런스 참가 신청 |
| Kafka 이벤트 스트리밍 | Spring Events → Kafka 전환 | `EventPublisher` 인터페이스 추상화 | 배민스토어 이벤트 기반 아키텍처 |
| 분산 락 (Redisson) | 더 정교한 분산 환경 제어 | Redis Lua Script으로 충분, 필요 시 전환 | |
| 모니터링 | Prometheus + Grafana 대시보드 | Actuator 엔드포인트 + HikariCP 메트릭 활성화 | 토스 서버 모니터링 전략 |
| 알림 서비스 | 주문 완료/타임딜 오픈 알림 | 이벤트 리스너에 확장 포인트 마련 | |
| Redis → DB Sync | 재고 운영 종료 시 Redis → DB 동기화 | 인터페이스 설계 | 후덥 기술블로그 쿠폰 재고 설계 |

---

## 13. 용어 정의 (Glossary)

| 용어 | 정의 |
|---|---|
| **타임딜 (TimeDeal)** | 특정 시간대에 한정 수량을 할인가로 판매하는 이벤트 |
| **딜 재고 (Deal Stock)** | 타임딜에 할당된 판매 수량. 일반 상품 재고와 별도 관리 |
| **동시성 제어** | 다수의 요청이 동시에 공유 자원(재고)에 접근할 때 데이터 정합성을 보장하는 메커니즘 |
| **낙관적 락** | 충돌이 드물다고 가정하고, 커밋 시점에 버전 비교로 충돌을 감지하는 방식 |
| **비관적 락** | 충돌을 사전에 방지하기 위해, 조회 시점에 DB 행 락을 거는 방식 |
| **Soft Delete** | 물리 삭제 대신 `deleted_at` 타임스탬프를 기록하여 논리적으로 삭제 처리 |
| **Cache Aside** | 캐시 미스 시 DB에서 조회 후 캐시에 적재하는 패턴 |
| **커서 페이지네이션** | 마지막 조회 항목의 ID를 기준으로 다음 페이지를 조회하는 방식 |
| **TPS** | Transactions Per Second. 초당 처리 가능한 트랜잭션 수 |
| **p95 / p99** | 전체 요청 중 95% / 99%가 이 시간 이내에 완료되는 응답시간 |

---

## 변경 이력 (Changelog)

| 버전 | 날짜 | 작성자 | 변경 내용 |
|---|---|---|---|
| v3.0.0 | 2026-02-18 | heygw44 | 7장 코딩 가이드라인 신규 추가 — OOP/SOLID 원칙 적용 가이드, 객체지향 생활체조 9원칙 체크리스트, 클린코드 네이밍/메서드 작성 원칙(우테코 기준), RESTful HTTP API 설계 원칙(메서드·상태코드·URI 규칙), 코드 품질 PR 체크리스트, Early Return/Guard Clause 코드 예시 |
| v2.0.0 | 2026-02-18 | heygw44 | 프로젝트명 FlashDeal → SnapStock 변경, 기술 블로그 레퍼런스 체계화(우아한형제들/쿠팡/토스/여기어때/사람인), HikariCP 커넥션 풀 설정 추가, Redis 직렬화 전략 추가, 동시성 비교 리포트 템플릿 추가, 캐싱 전략에 변경 빈도 기준 추가, 이벤트 확장 포인트 구체화, ADR-005 추가, 모니터링 섹션 강화 |
| v1.2.0 | 2026-02-18 | heygw44 | 패키지 구조, Git 전략/커밋 컨벤션, README 가이드, API 요청/응답 상세 예시 추가 |
| v1.1.0 | 2026-02-18 | heygw44 | Spring Boot 4.0.2 → 3.5.10 변경 (안정성·레퍼런스·실무 정합성 근거) |
| v1.0.0 | 2026-02-18 | heygw44 | 초안 작성 |
