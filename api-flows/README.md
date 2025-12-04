# API Flows

K8s 클러스터 내부 서비스 간 동기 API 호출 흐름을 정리한 문서입니다.

## 📋 목차

- [개요](#개요)
- [설계 원칙](#설계-원칙)
- [서비스별 API](#서비스별-api)
- [공통 규칙](#공통-규칙)

---

## 개요

이 디렉토리는 **서비스 간 동기 통신**을 위한 Internal API를 관리합니다.

**통신 방식:**
- **프로토콜**: HTTP REST API
- **인증**: Istio mTLS (클러스터 내부 전용)
- **네트워크**: Kubernetes Service를 통한 통신
- **스키마**: Avro 기반 요청/응답 정의

**비동기 이벤트와의 차이:**
- **동기 API** (이 문서): 즉각적인 응답이 필요한 데이터 조회/명령
- **비동기 이벤트** ([event-flows](../event-flows/)): SAGA 패턴, 이벤트 기반 상태 전파

---

## 설계 원칙

### 1. 최소한의 동기 통신
- 가능한 한 비동기 이벤트 기반으로 설계
- 동기 API는 다음 경우에만 사용:
  - **실시간 조회**: 사용자 정보, 재고 확인 등
  - **즉시 검증 필요**: 권한 체크, 중복 검사 등
  - **외부 API 호출**: 결제 게이트웨이, SMS 발송 등

### 2. Internal API만 관리
- **대상**: K8s 클러스터 내부 서비스 간 통신만 문서화
- **제외**: 외부 클라이언트(모바일/웹)용 Public API는 각 서비스에서 관리

### 3. 버전 관리
- URL Path에 버전 명시: `/internal/v1/...`
- Breaking Change 시 새 버전 생성 (`/internal/v2/...`)
- 구버전은 6개월 deprecation 기간 후 제거

### 4. 에러 처리
- 모든 API는 `ErrorResponse` 스키마 사용
- HTTP 상태 코드와 에러 코드를 함께 반환
- 상세 에러 정보는 `details` 필드 활용

---

## 서비스별 API

### 1. Customer Service
사용자 계정 및 프로필 정보 제공

**APIs:**
- [Internal User API](./customer-service/internal-user-api.md) - 사용자 조회

**주요 사용처:**
- Order Service: 주문 생성 시 사용자 검증
- Store Service: 매장 생성 시 OWNER 역할 확인

[📂 Customer Service 디렉토리](./customer-service/)

---

### 2. Order Service
주문 상세 정보 조회 (추후 추가 예정)

---

### 3. Store Service
매장 정보 조회

**APIs:**
- [Internal Store API](./store-service/internal-store-api.md) - 스토어 조회

**주요 사용처:**
- Product Service: 상품 등록 시 스토어 소유자 검증 및 스토어 이름 조회

[📂 Store Service 디렉토리](./store-service/)

---

### 4. Product Service
상품 정보 조회

**APIs:**
- [Internal Product API](./product-service/internal-product-api.md) - 상품 조회

**주요 사용처:**
- Order Service: 주문 생성 시 상품 정보 조회 (가격, 스토어 정보)
- Payment Service: 결제 시 상품 유효성 검증

[📂 Product Service 디렉토리](./product-service/)

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

### HTTP Method 사용
- **GET**: 조회 (멱등성 보장)
- **POST**: 생성 또는 복잡한 조회 (Body 필요 시)
- **PUT**: 전체 수정
- **PATCH**: 부분 수정
- **DELETE**: 삭제

### 공통 헤더
```http
Content-Type: application/json
Accept: application/json
X-Request-ID: {UUID}  # 요청 추적용 (선택)
```

### 응답 포맷

**성공 (200 OK):**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "username": "john_doe",
  "email": "john@example.com",
  ...
}
```

**실패 (4xx/5xx):**
```json
{
  "errorCode": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다",
  "timestamp": 1699999999999,
  "path": "/internal/v1/users/123e4567-e89b-12d3-a456-426614174000"
}
```

### 타임아웃 정책
- **Connection Timeout**: 3초
- **Read Timeout**: 5초
- **Retry**: 최대 2회 (GET 요청만)
- **Circuit Breaker**: 5분간 50% 이상 실패 시 OPEN

---

## 스키마 참조

모든 API의 요청/응답 스키마는 **Avro 스키마 파일**로 정의됩니다.

### Avro 스키마 위치
```
src/main/api/avro/
├── customer/        # Customer Service API 스키마
├── order/           # Order Service API 스키마
├── store/           # Store Service API 스키마
├── product/         # Product Service API 스키마
└── common/          # 공통 스키마 (ErrorResponse, Pagination 등)
```

### 생성된 Java Class 사용
```kotlin
// Order Service에서 Customer Service API 호출
import com.groom.ecommerce.customer.api.avro.UserInternalResponse

@Service
class OrderService(
    private val restTemplate: RestTemplate
) {
    fun validateUser(userId: String): UserInternalResponse {
        val url = "http://customer-service/internal/v1/users/$userId"
        return restTemplate.getForObject(url, UserInternalResponse::class.java)
            ?: throw UserNotFoundException(userId)
    }
}
```

---

## 관련 문서

- [비동기 이벤트 흐름](../event-flows/) - Kafka 이벤트 기반 SAGA 패턴
- [전체 API 명세](../docs/interface/internal-api-specifications.md) - API 목록 및 상세
- [README](../README.md) - 프로젝트 메인 문서

---

## 문서 컨벤션

### 파일 명명 규칙
- `README.md`: 해당 서비스의 전체 API 목록
- `{api-name}.md`: 개별 API 상세 문서

### API 문서 구조
각 API 문서는 다음 섹션을 포함합니다:
1. **개요**: API 목적 및 사용처
2. **엔드포인트**: HTTP Method, Path, 파라미터
3. **요청/응답 스키마**: Avro 스키마 파일 링크
4. **시퀀스 다이어그램**: 서비스 간 호출 흐름
5. **에러 응답**: 가능한 에러 케이스
6. **사용 예시**: Kotlin/Java 코드 샘플
7. **성능 고려사항**: 캐싱, 타임아웃 등
