# Customer Service Internal API

## 개요

Customer Service는 사용자 계정 및 프로필 정보를 관리하며, 다른 서비스에 사용자 데이터를 제공합니다.

**책임:**
- 사용자 인증/인가 (JWT 토큰 검증)
- 사용자 프로필 관리
- 역할 기반 권한 검증 (CUSTOMER, OWNER, ADMIN)

---

## API 목록

### 1. Internal User API
사용자 정보 조회 API

**엔드포인트:** `GET /internal/v1/users/{userId}`

**사용처:**
- Order Service: 주문 생성 시 사용자 검증
- Store Service: 매장 생성 시 OWNER 역할 확인
- Payment Service: 결제 정보 조회 시 사용자 확인

[📄 상세 문서](./internal-user-api.md)

---

## 공통 스키마

모든 Customer Service API에서 사용하는 공통 타입:

### UserRole (Enum)
```json
{
  "type": "enum",
  "name": "UserRole",
  "symbols": ["CUSTOMER", "OWNER", "ADMIN"]
}
```

**역할 설명:**
- `CUSTOMER`: 일반 고객 (상품 주문 가능)
- `OWNER`: 매장 사장님 (매장 및 상품 관리 가능)
- `ADMIN`: 시스템 관리자 (전체 관리 권한)

---

## 보안 정책

### 인증
- **Istio mTLS**: 클러스터 내부 통신만 허용
- **Network Policy**: customer-service는 외부 직접 접근 불가
- **응답 필터링**: 민감한 정보(비밀번호 해시, 리프레시 토큰) 제외

### 데이터 보호
- **개인정보**: 실명, 전화번호, 주소는 GDPR 준수
- **로그**: 개인정보는 마스킹 처리 (`john_doe` → `j***_doe`)
- **캐싱**: Redis 캐시 TTL 5분, 민감 정보는 캐싱 금지

---

## 성능 최적화

### 캐싱 전략
```kotlin
@Cacheable(value = ["user"], key = "#userId", unless = "#result == null")
fun getUserById(userId: String): UserInternalResponse
```

**캐시 정책:**
- **TTL**: 5분
- **Eviction**: 사용자 정보 업데이트 시 즉시 무효화
- **Fallback**: 캐시 실패 시 DB 직접 조회

### DB 인덱싱
```sql
CREATE INDEX idx_user_id ON users(user_id);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);
```

---

## 에러 처리

### 공통 에러 코드

| HTTP Status | Error Code | 설명 |
|------------|-----------|------|
| 400 | `INVALID_UUID_FORMAT` | userId가 UUID 형식이 아님 |
| 404 | `USER_NOT_FOUND` | 사용자가 존재하지 않음 |
| 403 | `USER_INACTIVE` | 탈퇴한 사용자 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

### 에러 응답 예시
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

---

## 관련 문서

- [Avro 스키마](../../src/main/api/avro/customer/) - Customer Service API 스키마
- [Internal User API 상세](./internal-user-api.md) - 사용자 조회 API
