# Internal API 명세서

## 문서 개요

**작성일**: 2024-11-17
**버전**: 1.0
**대상 독자**: 백엔드 개발자, API 설계자

**목적**:
- K8s 클러스터 내부 서비스 간 동기 API 명세 제공
- 요청/응답 스키마 및 에러 처리 가이드
- 서비스 간 통신 계약 정의

---

## 📋 목차

1. [개요](#개요)
2. [공통 규칙](#공통-규칙)
3. [서비스별 API](#서비스별-api)
4. [공통 스키마](#공통-스키마)
5. [에러 처리](#에러-처리)

---

## 개요

### Internal API란?

**Internal API**는 K8s 클러스터 내부에서만 사용되는 서비스 간 통신 API입니다.

**특징:**
- **접근 제어**: Istio mTLS + Network Policy
- **프로토콜**: HTTP REST
- **스키마**: Avro 기반 타입 정의
- **버전 관리**: URL Path 버전 (`/internal/v1/...`)

### 비동기 이벤트와의 차이

| 구분 | Internal API (동기) | Kafka Event (비동기) |
|-----|-------------------|---------------------|
| **용도** | 즉시 응답 필요 (조회, 검증) | 상태 전파, SAGA 패턴 |
| **프로토콜** | HTTP REST | Kafka Message |
| **응답** | 동기 응답 (200 OK) | 비동기 처리 (Eventual Consistency) |
| **에러 처리** | HTTP 상태 코드 + ErrorResponse | 보상 트랜잭션 이벤트 |
| **문서 위치** | `api-flows/` | `event-flows/` |

---

## 공통 규칙

### URL 패턴

```
/{service-name}/internal/v{version}/{resource}
```

**예시:**
- `GET /customer-service/internal/v1/users/{userId}`
- `GET /order-service/internal/v1/orders/{orderId}`
- `GET /store-service/internal/v1/stores/{storeId}`

### HTTP Method

- **GET**: 조회 (멱등성 보장)
- **POST**: 생성 또는 복잡한 조회
- **PUT**: 전체 수정
- **PATCH**: 부분 수정
- **DELETE**: 삭제

### 공통 헤더

**Request:**
```http
Content-Type: application/json
Accept: application/json
X-Request-ID: {UUID}  # 선택, 요청 추적용
```

**Response:**
```http
Content-Type: application/json
X-Request-ID: {UUID}  # 요청과 동일한 ID
X-Cache-Hit: true|false  # 캐시 히트 여부 (선택)
```

### 타임아웃 정책

- **Connection Timeout**: 3초
- **Read Timeout**: 5초
- **Retry**: 최대 2회 (GET 요청만, 멱등성 보장)
- **Circuit Breaker**: 5분간 50% 이상 실패 시 OPEN

---

## 서비스별 API

### 1. Customer Service

사용자 계정 및 프로필 정보 제공

#### 1.1 사용자 조회 API

**엔드포인트:**
```http
GET /customer-service/internal/v1/users/{userId}
```

**응답 스키마:**
[`UserInternalResponse.avsc`](../../src/main/api/avro/customer/UserInternalResponse.avsc)

**상세 문서:**
[Internal User API](../../api-flows/customer-service/internal-user-api.md)

**사용처:**
- Order Service: 주문 생성 시 사용자 검증
- Store Service: 매장 생성 시 OWNER 역할 확인
- Payment Service: 결제 정보 조회

---

### 2. Order Service

주문 상세 정보 제공 (추후 추가 예정)

---

### 3. Store Service

매장 정보 제공 (추후 추가 예정)

---

### 4. Product Service

재고 정보 제공 (추후 추가 예정)

---

## 공통 스키마

모든 Internal API에서 사용하는 공통 Avro 스키마

### ErrorResponse

**스키마:** [`ErrorResponse.avsc`](../../src/main/api/avro/common/ErrorResponse.avsc)

**용도:** 모든 에러 응답에 사용

**예시:**
```json
{
  "errorCode": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다",
  "timestamp": 1699999999999,
  "path": "/internal/v1/users/123e4567-e89b-12d3-a456-426614174000",
  "details": {
    "userId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

### Pagination

**스키마:** [`Pagination.avsc`](../../src/main/api/avro/common/Pagination.avsc)

**용도:** 리스트 조회 API 페이지네이션

**예시:**
```json
{
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## 에러 처리

### HTTP 상태 코드

| Status Code | 설명 | 사용 케이스 |
|------------|------|------------|
| 200 OK | 성공 | 조회 성공 |
| 201 Created | 생성 성공 | 리소스 생성 |
| 400 Bad Request | 잘못된 요청 | 파라미터 검증 실패 |
| 403 Forbidden | 권한 없음 | 역할 검증 실패 |
| 404 Not Found | 리소스 없음 | 데이터 조회 실패 |
| 500 Internal Server Error | 서버 오류 | 예상치 못한 오류 |
| 503 Service Unavailable | 서비스 불가 | Circuit Breaker OPEN |

### 에러 코드 네이밍 규칙

```
{RESOURCE}_{ERROR_TYPE}
```

**예시:**
- `USER_NOT_FOUND`
- `INVALID_UUID_FORMAT`
- `STORE_NOT_FOUND`
- `ORDER_ALREADY_CANCELLED`
- `INSUFFICIENT_STOCK`

### 에러 응답 구조

모든 에러는 `ErrorResponse` 스키마를 따릅니다.

**필수 필드:**
- `errorCode`: 에러 코드 (대문자 + 언더스코어)
- `message`: 사용자에게 표시할 메시지
- `timestamp`: 에러 발생 시각 (epoch millis)
- `path`: 요청 경로

**선택 필드:**
- `details`: 추가 에러 상세 정보 (Map<String, String>)

---

## 성능 최적화

### 캐싱 전략

**Redis 캐시:**
- **TTL**: 5분 (기본값)
- **Key 패턴**: `{service}:{resource}:{id}`
- **Eviction**: 리소스 업데이트 시 즉시 무효화

**예시:**
```kotlin
@Cacheable(value = ["user"], key = "#userId", unless = "#result == null")
fun getUserById(userId: String): UserInternalResponse
```

### DB 인덱싱

**필수 인덱스:**
- Primary Key 컬럼
- 자주 조회되는 컬럼 (user_id, email, order_id 등)

**복합 인덱스:**
- 범위 조회 + 정렬이 함께 사용되는 경우

### 모니터링

**메트릭:**
- 응답 시간 (95th percentile < 100ms 목표)
- 에러율 (< 1% 목표)
- 캐시 히트율 (> 80% 목표)

**로그:**
- 요청 ID 기반 분산 추적 (X-Request-ID)
- 개인정보 마스킹 처리

---

## 보안

### 인증/인가

- **Istio mTLS**: 클러스터 내부 통신 암호화
- **Network Policy**: 서비스별 접근 제어
- **응답 필터링**: 민감 정보 제외 (비밀번호, 토큰 등)

### 데이터 보호

- **GDPR 준수**: 개인정보 처리 로그 기록
- **로그 마스킹**: 민감 정보 자동 마스킹
- **캐시 보안**: 민감 정보는 캐싱 금지

---

## 관련 문서

- [API Flows](../../api-flows/README.md) - 동기 API 흐름 문서
- [Event Flows](../../event-flows/README.md) - 비동기 이벤트 흐름 문서
- [Avro API 스키마](../../src/main/api/avro/) - API Avro 스키마 파일
