# Quick Start Guide

## 프로젝트 개요

이 프로젝트는 **Kotlin 기반 단일 모듈** Spring Cloud Contract Hub입니다.

### 주요 특징
- ✅ **Kotlin DSL**로 작성된 Spring Cloud Contract
- ✅ **단일 모듈** 구조로 간편한 관리
- ✅ **Avro 스키마** 기반 이벤트 명세 자동 생성
- ✅ **이벤트 플로우 문서** 자동 업데이트

---

## 빌드 구조

### 단일 모듈 구성
```
c4ang-contract-hub/  (루트 프로젝트)
├── build.gradle.kts          # 단일 빌드 파일
├── settings.gradle.kts        # 단일 모듈 설정
├── contracts/                 # Kotlin DSL Contract
├── src/main/avro/            # Avro 스키마
├── event-flows/              # 이벤트 플로우 문서
└── docs/                     # 가이드 문서
```

### Spring Cloud Contract 설정
build.gradle.kts에서 Contract 설정:
```kotlin
contracts {
    testFramework.set(JUNIT5)
    testMode.set(EXPLICIT)
    baseClassForTests.set("com.c4ang.contract.BaseContractTest")
    contractsDslDir.set(file("src/contractTest/resources/contracts"))
}
```

**참고**:
- Spring Cloud Contract 플러그인이 자동으로 `contractTest` source set 생성
- `src/contractTest/resources/contracts/` 디렉토리를 자동 인식
- 별도 source set 설정 불필요

**장점**:
- ✅ 표준 Gradle 프로젝트 구조 준수
- ✅ Contract 관련 코드와 리소스를 명확히 분리
- ✅ IDE에서 테스트 소스로 자동 인식

---

## 이벤트 명세 관리 워크플로우

### 기존 문제점 (3곳 수동 관리)
1. Avro 스키마 수정
2. Spring Cloud Contract 수정
3. 이벤트 플로우 문서 수정

### 개선 후 (1곳만 수정)
1. **Avro 스키마만 수정**
2. 빌드 실행 → 자동으로 문서 업데이트

---

## 사용 방법

### 1. 새로운 이벤트 추가

#### Step 1: Avro 스키마 정의
```bash
# src/main/avro/events/OrderShippedEvent.avsc 생성
```

```json
{
  "type": "record",
  "namespace": "com.c4ang.events.order",
  "name": "OrderShippedEvent",
  "doc": "주문 배송 시작 이벤트",
  "fields": [
    {
      "name": "metadata",
      "type": "com.c4ang.events.common.EventMetadata",
      "doc": "이벤트 메타데이터"
    },
    {
      "name": "orderId",
      "type": "string",
      "doc": "주문 ID"
    },
    {
      "name": "trackingNumber",
      "type": "string",
      "doc": "배송 추적 번호"
    }
  ]
}
```

#### Step 2: 빌드 실행
```bash
./gradlew build
```

이 명령어가 자동으로 수행하는 작업:
- ✅ Avro Java 클래스 생성
- ✅ 이벤트 명세 문서 생성 (`docs/generated/event-specifications.md`)
- ✅ 이벤트 플로우 문서 자동 업데이트 (AUTO_GENERATED 섹션)

#### Step 3: 이벤트 플로우 문서에 비즈니스 정보 추가
```markdown
<!-- event-flows/order-saga/README.md -->

#### 7. OrderShippedEvent
**발행자**: Shipment Service
**구독자**: Notification Service
**트리거 조건**: 배송이 시작되었을 때
**비즈니스 로직**: 고객에게 배송 시작 알림 발송
```

### 2. 기존 이벤트 수정

#### Step 1: Avro 스키마 수정
```json
// src/main/avro/events/OrderCreatedEvent.avsc
{
  "fields": [
    // ... 기존 필드들
    {
      "name": "deliveryAddress",  // 새 필드 추가
      "type": "string",
      "doc": "배송지 주소"
    }
  ]
}
```

#### Step 2: 빌드 실행
```bash
./gradlew generateAvroEventDocs
```

→ 이벤트 플로우 문서의 필드 테이블이 자동으로 업데이트됩니다!

---

## Avro 문서 자동 생성 상세

### 생성되는 파일

#### 1. 전체 이벤트 명세 문서
```
docs/generated/event-specifications.md
```

모든 Avro 이벤트의 전체 명세 (자동 생성)

#### 2. 이벤트 플로우 문서 자동 섹션
```
event-flows/**/README.md
```

AUTO_GENERATED 마커 사이의 섹션만 자동 업데이트

### AUTO_GENERATED 마커 사용법

```markdown
## 이벤트 명세

<!-- AUTO_GENERATED_EVENT_SPEC_START -->
<!-- 이 사이의 내용은 자동 생성됩니다 -->
<!-- AUTO_GENERATED_EVENT_SPEC_END -->

## 수동 작성 섹션
(다이어그램, 비즈니스 로직 등)
```

### 생성되는 내용 예시

```markdown
### 1. OrderCreatedEvent

**Kafka 토픽**: `c4ang.order.created`

**Avro 스키마**: `src/main/avro/events/OrderCreatedEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `customerId` | string | ✅ | 고객 ID |
| `productId` | string | ✅ | 상품 ID |
| `quantity` | int | ✅ | 주문 수량 |
| `totalAmount` | Decimal | ✅ | 주문 총액 |
| `orderStatus` | Enum: PENDING_PAYMENT, ... | ✅ | 주문 상태 |
```

---

## Spring Cloud Contract와 Avro 통합

### 현재 방식 (수동)
```kotlin
// contracts/messaging/order_created_event.kts
contract {
    outputMessage {
        sentTo("c4ang.order.created")
        body("""
            {
                "orderId": "ORD-12345",
                "customerId": "CUST-001"
            }
        """.trimIndent())
    }
}
```

### 권장 방식 (Avro 활용)
```kotlin
// contracts/messaging/order_created_event.kts
import com.c4ang.events.order.OrderCreatedEvent
import com.c4ang.events.common.EventMetadata

contract {
    description = "주문 생성 이벤트 - Avro 기반"

    input {
        triggeredBy("triggerOrderCreatedEvent()")
    }

    outputMessage {
        sentTo("c4ang.order.created")

        // Avro 객체를 직접 사용
        body(
            OrderCreatedEvent.newBuilder()
                .setMetadata(...)
                .setOrderId("ORD-12345")
                .build()
        )
    }
}
```

**장점**:
- ✅ Avro 스키마 변경 시 컴파일 에러 발생
- ✅ 타입 안정성 보장
- ✅ IDE 자동완성 지원

---

## Gradle 태스크

### Avro 관련
```bash
# Avro Java 클래스 생성
./gradlew generateAvroJava

# Avro 문서 자동 생성 (이벤트 플로우 문서 업데이트 포함)
./gradlew generateAvroEventDocs
```

### Spring Cloud Contract 관련
```bash
# Contract 테스트 실행
./gradlew contractTest

# Stub 생성 및 로컬 배포
./gradlew publishToMavenLocal

# 전체 빌드 (Avro 문서 자동 생성 포함)
./gradlew build
```

---

## 문서 작성 가이드

### 자동 생성되는 부분 (수정 X)
- ✅ 이벤트 필드 테이블
- ✅ Kafka 토픽명
- ✅ Avro 스키마 파일 경로
- ✅ 필드 타입 및 필수 여부

### 수동 작성하는 부분
- ✅ Mermaid 다이어그램 (비즈니스 플로우)
- ✅ 발행자/구독자 정보
- ✅ 트리거 조건
- ✅ 비즈니스 로직 설명
- ✅ 상태 전이도
- ✅ 타임아웃/재시도 정책
- ✅ 보상 트랜잭션 설명

---

## 개발 체크리스트

### 새로운 이벤트 추가 시
- [ ] Avro 스키마 작성 (`src/main/avro/events/*.avsc`)
- [ ] `./gradlew generateAvroEventDocs` 실행
- [ ] 이벤트 플로우 문서에 비즈니스 정보 추가
- [ ] Mermaid 다이어그램 업데이트
- [ ] Spring Cloud Contract 작성 (선택)
- [ ] Contract 테스트 실행

### 기존 이벤트 수정 시
- [ ] Avro 스키마 수정
- [ ] 하위 호환성 검토 (필수!)
- [ ] `./gradlew generateAvroEventDocs` 실행
- [ ] 자동 업데이트된 문서 확인
- [ ] Spring Cloud Contract 업데이트 (필요시)
- [ ] Contract 테스트 실행

---

## 자주 묻는 질문

### Q1. Avro 스키마를 수정했는데 문서가 업데이트되지 않아요.
```bash
# 빌드를 실행하세요
./gradlew generateAvroEventDocs
```

### Q2. AUTO_GENERATED 섹션이 비어있어요.
README에서 이벤트 이름이 한 번이라도 언급되어야 자동 생성됩니다.
예: "OrderCreatedEvent", "PaymentCompletedEvent" 등

### Q3. Spring Cloud Contract에서 Avro 객체 사용이 복잡해요.
Phase 1에서는 기존 JSON 방식 유지 가능합니다. 점진적으로 마이그레이션하세요.

### Q4. Decimal 타입을 Contract에서 어떻게 사용하나요?
```kotlin
import java.math.BigDecimal
import java.nio.ByteBuffer

.setTotalAmount(
    BigDecimal("50000.00").toByteBuffer()
)

// Extension 함수
fun BigDecimal.toByteBuffer(): ByteBuffer {
    return ByteBuffer.wrap(this.unscaledValue().toByteArray())
}
```

---

## 다음 단계

### Phase 1 (현재) ✅
- ✅ AvroDocGenerator 개선
- ✅ 이벤트 플로우 문서 자동 업데이트

### Phase 2 (향후)
- [ ] Spring Cloud Contract를 Avro 기반으로 마이그레이션
- [ ] 공통 샘플 데이터 생성기 구현

### Phase 3 (장기)
- [ ] Avro 스키마 메타데이터 확장
- [ ] Saga 플로우 다이어그램 부분 자동화

---
## 추가 리소스

- [Avro 통합 전략 상세 문서](./avro-integration-strategy.md)
- [Gradle buildSrc 가이드](./gradle-buildSrc-guide.md)
- [Contract 작성 가이드](../src/contractTest/resources/contracts/README.md)
- [이벤트 플로우 가이드](../event-flows/README.md)
- [Spring Cloud Contract 공식 문서](https://spring.io/projects/spring-cloud-contract)
- [Apache Avro 공식 문서](https://avro.apache.org/docs/current/)
