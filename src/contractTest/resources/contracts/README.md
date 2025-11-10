# Spring Cloud Contract 가이드

## 개요

이 디렉토리는 C4ang MSA 시스템의 서비스 간 API Contract를 관리합니다. Spring Cloud Contract를 사용하여 Producer-Consumer 간의 계약을 정의하고 검증합니다.

**이 프로젝트는 Kotlin DSL을 사용합니다.**

## 디렉토리 구조

```
contracts/
├── README.md                      # 이 문서
├── order-service/                 # 주문 서비스 계약
│   ├── create_order.kts
│   ├── get_order.kts
│   └── get_order_not_found.kts
├── payment-service/               # 결제 서비스 계약
│   ├── process_payment.kts
│   └── process_payment_validation_error.kts
├── inventory-service/             # 재고 서비스 계약
│   ├── reserve_inventory.kts
│   └── reserve_inventory_out_of_stock.kts
└── notification-service/          # 알림 서비스 계약
```

## Contract 작성 방법

### 1. 기본 구조

**파일 확장자**: `.kts` (Kotlin Script)

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "명확한 설명"

    request {
        method = {HTTP_METHOD}
        url = url("{endpoint-path}")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "field1": "value1",
                "field2": "value2"
            }
        """.trimIndent())
    }

    response {
        status = {HTTP_STATUS_CODE}

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "field1": "value1",
                "field2": "value2"
            }
        """.trimIndent())
    }
}
```

### 2. 명명 규칙

- **파일명**: `{action}_{resource}_{scenario}.kts`
  - 예: `create_order.kts`, `get_order_not_found.kts`
- **Description**: 한글로 명확하게 작성
  - 예: "주문 생성 API - 정상 케이스"

### 3. HTTP 메서드별 예시

#### GET 요청

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 조회 API - 정상 케이스"

    request {
        method = GET
        url = url("/api/v1/orders/ORD-12345")
    }

    response {
        status = OK

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12345",
                "productId": "PROD-001",
                "quantity": 2,
                "status": "COMPLETED",
                "createdAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:00:00Z"))}",
                "updatedAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:05:00Z"))}"
            }
        """.trimIndent())
    }
}
```

#### POST 요청

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 생성 API - 정상 케이스"

    request {
        method = POST
        url = url("/api/v1/orders")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "productId": "PROD-001",
                "quantity": 2,
                "customerId": "CUST-12345"
            }
        """.trimIndent())
    }

    response {
        status = CREATED

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "${value(client(regex("[A-Z]+-[0-9]+")), server("ORD-12345"))}",
                "productId": "${fromRequest().body("$.productId")}",
                "quantity": ${fromRequest().body("$.quantity")},
                "customerId": "${fromRequest().body("$.customerId")}",
                "status": "CREATED",
                "createdAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:00:00Z"))}"
            }
        """.trimIndent())
    }
}
```

#### PUT 요청

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 상태 업데이트 API"

    request {
        method = PUT
        url = url("/api/v1/orders/ORD-12345/status")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "status": "CANCELLED"
            }
        """.trimIndent())
    }

    response {
        status = OK

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12345",
                "status": "CANCELLED",
                "updatedAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:00:00Z"))}"
            }
        """.trimIndent())
    }
}
```

#### DELETE 요청

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 삭제 API"

    request {
        method = DELETE
        url = url("/api/v1/orders/ORD-12345")
    }

    response {
        status = NO_CONTENT
    }
}
```

### 4. 동적 값 패턴

Contract에서 자주 사용하는 동적 값 패턴:

```kotlin
// 정규식 패턴 - ID
"orderId": "${value(client(regex("[A-Z]+-[0-9]+")), server("ORD-12345"))}"

// ISO 날짜/시간
"createdAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:00:00Z"))}"

// 이메일
"email": "${value(client(regex(email())), server("test@example.com"))}"

// UUID
"id": "${value(client(regex(uuid())), server("550e8400-e29b-41d4-a716-446655440000"))}"

// 숫자
"amount": ${value(client(anyNumber()), server(10000))}

// Boolean
"isActive": ${value(client(anyBoolean()), server(true))}

// Request 값 재사용
"productId": "${fromRequest().body("$.productId")}"
"quantity": ${fromRequest().body("$.quantity")}
```

### 5. 에러 케이스 정의

#### 400 Bad Request (유효성 검증 실패)

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "결제 처리 API - 유효성 검증 실패"

    request {
        method = POST
        url = url("/api/v1/payments")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "",
                "amount": -1000,
                "paymentMethod": ""
            }
        """.trimIndent())
    }

    response {
        status = BAD_REQUEST

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "error": "VALIDATION_ERROR",
                "message": "입력 값이 유효하지 않습니다",
                "details": [
                    {
                        "field": "orderId",
                        "message": "주문 ID는 필수입니다"
                    },
                    {
                        "field": "amount",
                        "message": "금액은 0보다 커야 합니다"
                    },
                    {
                        "field": "paymentMethod",
                        "message": "결제 수단은 필수입니다"
                    }
                ]
            }
        """.trimIndent())
    }
}
```

#### 404 Not Found

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 조회 API - 존재하지 않는 주문"

    request {
        method = GET
        url = url("/api/v1/orders/INVALID-ID")
    }

    response {
        status = NOT_FOUND

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "error": "NOT_FOUND",
                "message": "주문을 찾을 수 없습니다",
                "orderId": "INVALID-ID"
            }
        """.trimIndent())
    }
}
```

#### 409 Conflict

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "재고 예약 API - 재고 부족"

    request {
        method = POST
        url = url("/api/v1/inventory/reservations")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12346",
                "productId": "PROD-999",
                "quantity": 100
            }
        """.trimIndent())
    }

    response {
        status = CONFLICT

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "error": "OUT_OF_STOCK",
                "message": "재고가 부족합니다",
                "productId": "${fromRequest().body("$.productId")}",
                "requestedQuantity": ${fromRequest().body("$.quantity")},
                "availableQuantity": 0
            }
        """.trimIndent())
    }
}
```

### 6. 인증/인가

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "인증이 필요한 주문 조회 API"

    request {
        method = GET
        url = url("/api/v1/orders/ORD-12345")

        headers {
            header("Authorization", "Bearer ${value(client(regex("[A-Za-z0-9-._~+/]+=*")), server("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))}")
        }
    }

    response {
        status = OK

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12345",
                "customerId": "CUST-001"
            }
        """.trimIndent())
    }
}
```

### 7. 페이지네이션

```kotlin
import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 목록 조회 - 페이지네이션"

    request {
        method = GET
        url = url("/api/v1/orders?page=0&size=10")
    }

    response {
        status = OK

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "content": [
                    {
                        "orderId": "ORD-001",
                        "status": "COMPLETED"
                    },
                    {
                        "orderId": "ORD-002",
                        "status": "PENDING"
                    }
                ],
                "pageable": {
                    "pageNumber": 0,
                    "pageSize": 10
                },
                "totalElements": ${value(client(anyNumber()), server(25))},
                "totalPages": ${value(client(anyNumber()), server(3))}
            }
        """.trimIndent())
    }
}
```

## Contract 테스트 실행

### 로컬에서 테스트

```bash
# 모든 Contract 테스트 실행
./gradlew contractTest

# 특정 서비스의 Contract만 테스트
./gradlew test --tests "*OrderService*"

# Contract 검증 및 Stub 생성
./gradlew verifierStubsJar
```

### Stub 생성 및 배포

```bash
# Local Maven Repository에 Stub 배포
./gradlew publishToMavenLocal

# Remote Repository에 Stub 배포 (설정 필요)
./gradlew publish
```

## Consumer 측에서 Stub 사용

### 1. 의존성 추가

```kotlin
dependencies {
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}
```

### 2. 테스트 설정

```java
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

@AutoConfigureStubRunner(
    ids = "com.c4ang:order-service:+:stubs:8080",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCreateOrder() {
        // Stub이 8080 포트에서 자동으로 실행됨
        // order-service의 실제 구현 없이 테스트 가능

        webTestClient.post()
            .uri("http://localhost:8080/api/v1/orders")
            .bodyValue(Map.of(
                "productId", "PROD-001",
                "quantity", 2,
                "customerId", "CUST-12345"
            ))
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.orderId").exists()
            .jsonPath("$.status").isEqualTo("CREATED");
    }
}
```

## 서비스별 Contract 작성 규칙

### Order Service

- **엔드포인트**: `/api/v1/orders/**`
- **주요 Contract**:
  - 주문 생성 (POST)
  - 주문 조회 (GET)
  - 주문 목록 조회 (GET with pagination)
  - 주문 상태 업데이트 (PUT)
  - 주문 취소 (DELETE)

### Payment Service

- **엔드포인트**: `/api/v1/payments/**`
- **주요 Contract**:
  - 결제 처리 (POST)
  - 결제 조회 (GET)
  - 결제 취소/환불 (POST)

### Inventory Service

- **엔드포인트**: `/api/v1/inventory/**`
- **주요 Contract**:
  - 재고 조회 (GET)
  - 재고 예약 (POST)
  - 재고 예약 취소 (DELETE)
  - 재고 업데이트 (PUT)

### Notification Service

- **엔드포인트**: `/api/v1/notifications/**`
- **주요 Contract**:
  - 알림 발송 (POST)
  - 알림 조회 (GET)

## Kotlin DSL 장점

### 1. 타입 안정성

```kotlin
// 컴파일 타임에 타입 체크
status = CREATED      // ✅ OK
status = "CREATED"    // ❌ 컴파일 에러
status = 201          // ❌ 컴파일 에러
```

### 2. IDE 자동완성

- IntelliJ IDEA에서 완벽한 자동완성 지원
- HTTP 메서드, 상태 코드 등 모든 요소에 자동완성
- 오타 방지 및 생산성 향상

### 3. Null Safety

```kotlin
// Kotlin의 null safety 활용
val orderId: String = fromRequest().body("$.orderId")
```

### 4. 간결한 문법

```kotlin
// JSON을 String Template으로 깔끔하게 작성
body = body("""
    {
        "orderId": "${value(client(regex("[A-Z]+-[0-9]+")), server("ORD-12345"))}",
        "status": "CREATED"
    }
""".trimIndent())
```

## 체크리스트

새로운 Contract 작성 시 확인사항:

- [ ] 명확한 description 작성
- [ ] 적절한 HTTP 메서드 사용 (GET, POST, PUT, DELETE)
- [ ] Request/Response 필드가 실제 API와 일치
- [ ] 동적 값에 적절한 정규식 패턴 사용
- [ ] 정상 케이스와 에러 케이스 모두 정의
- [ ] Content-Type 헤더 명시
- [ ] 파일명 규칙 준수 (`{action}_{resource}_{scenario}.kts`)
- [ ] Contract 테스트 실행 및 통과 확인
- [ ] JSON 문자열에 `.trimIndent()` 사용

## 문제 해결

### Contract 테스트 실패 시

1. **Request/Response 불일치**
   - Producer의 실제 구현과 Contract가 일치하는지 확인
   - JSON 필드명, 타입, 필수/선택 여부 확인

2. **정규식 패턴 오류**
   - 동적 값의 정규식이 실제 응답과 매칭되는지 확인
   - `value(client(...), server(...))` 패턴 검증

3. **Base Test Class 문제**
   - `BaseContractTest` 클래스가 올바르게 설정되었는지 확인
   - Test context가 정상적으로 로드되는지 확인

4. **Kotlin Script 컴파일 오류**
   - IntelliJ에서 Kotlin 플러그인 활성화 확인
   - `./gradlew clean build` 후 재시도

## 참고 자료

- [Spring Cloud Contract 공식 문서](https://spring.io/projects/spring-cloud-contract)
- [Contract DSL 참조](https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#contract-dsl)
- [Kotlin DSL 가이드](https://kotlinlang.org/docs/type-safe-builders.html)
- [Contract 작성 상세 가이드라인](../docs/contract-guidelines.md)
