# Avro 스키마 활용 전략

## 현재 문제점

프로젝트에서 이벤트 데이터 명세가 3곳에서 중복 관리되고 있습니다:

### 1. Avro 스키마 (Single Source of Truth 후보)
```
src/main/avro/events/OrderCreatedEvent.avsc
```
- 실제 Kafka 메시지의 직렬화/역직렬화에 사용
- Schema Registry에 등록되어 버전 관리
- Java 클래스 자동 생성

### 2. Spring Cloud Contract Messaging
```kotlin
// contracts/messaging/order_created_event.kts
body("""
    {
        "orderId": "ORD-12345",
        "customerId": "CUST-001",
        ...
    }
""".trimIndent())
```
- Producer-Consumer 계약 테스트
- JSON으로 메시지 구조를 수동 정의
- **Avro 스키마와 동기화 필요**

### 3. 이벤트 플로우 문서
```markdown
// event-flows/order-saga/README.md
**페이로드** (Avro 직렬화):
{
  "orderId": "ORD-12345",
  ...
}
```
- 수동으로 작성된 이벤트 명세
- **Avro 스키마와 동기화 필요**

## 문제점 요약

**관리 포인트가 3곳**으로 분산:
1. Avro 스키마 수정
2. Spring Cloud Contract 수정
3. 이벤트 플로우 문서 수정

→ **명세 변경 시 3곳 모두 수동 업데이트 필요** (휴먼 에러 가능성 ⬆️)

---

## 개선 방안

### 🎯 핵심 전략: Avro 스키마를 단일 진실 공급원(Single Source of Truth)으로 사용

```
Avro 스키마 (.avsc)
    ↓
    ├─→ Java 클래스 생성 (Gradle Avro Plugin)
    ├─→ Spring Cloud Contract 자동 생성
    └─→ 이벤트 플로우 문서 자동 생성
```

### 방안 1: Spring Cloud Contract에서 Avro 활용

#### 현재 방식 (수동 동기화 필요)
```kotlin
// contracts/messaging/order_created_event.kts
contract {
    outputMessage {
        sentTo("c4ang.order.created")
        body("""
            {
                "orderId": "ORD-12345",
                "customerId": "CUST-001",
                ...
            }
        """.trimIndent())
    }
}
```

#### 개선 방식 1: Avro 기반 Contract (권장 ⭐)
```kotlin
// contracts/messaging/order_created_event.kts
import com.c4ang.events.order.OrderCreatedEvent
import com.c4ang.events.common.EventMetadata
import org.apache.avro.specific.SpecificRecordBase

contract {
    description = "주문 생성 이벤트 - Avro 기반 Contract"

    input {
        triggeredBy("triggerOrderCreatedEvent()")
    }

    outputMessage {
        sentTo("c4ang.order.created")

        // Avro 객체를 직접 사용
        body(
            OrderCreatedEvent.newBuilder()
                .setMetadata(EventMetadata.newBuilder()
                    .setEventId(anyAlphaNumeric())
                    .setEventType("OrderCreated")
                    .setTimestamp(anyNumber())
                    .setCorrelationId("ORD-12345")
                    .setVersion("1.0")
                    .setSource("order-service")
                    .build())
                .setOrderId("ORD-12345")
                .setCustomerId("CUST-001")
                .setProductId("PROD-001")
                .setQuantity(2)
                .setTotalAmount(BigDecimal("50000.00").toByteBuffer())
                .setOrderStatus(OrderStatus.PENDING_PAYMENT)
                .build()
        )

        headers {
            // Avro 직렬화 헤더
            header("content-type", "application/avro")
        }
    }
}
```

**장점**:
- ✅ **Avro 스키마가 변경되면 컴파일 에러 발생** (타입 안정성)
- ✅ Contract 테스트 실행 시 Avro 스키마 호환성 자동 검증
- ✅ IDE 자동완성 지원
- ✅ 리팩토링 시 안전성 보장

**단점**:
- Contract에서 Avro 생성 클래스에 의존성 추가
- Decimal 등 복잡한 타입 변환 필요

#### 개선 방식 2: 공통 샘플 데이터 활용
```kotlin
// buildSrc/src/main/kotlin/AvroSampleGenerator.kt
object AvroSampleGenerator {
    fun generateOrderCreatedEventSample(): OrderCreatedEvent {
        return OrderCreatedEvent.newBuilder()
            .setMetadata(...)
            .setOrderId("ORD-12345")
            .build()
    }
}

// contracts/messaging/order_created_event.kts
contract {
    outputMessage {
        sentTo("c4ang.order.created")
        body(AvroSampleGenerator.generateOrderCreatedEventSample())
    }
}
```

**장점**:
- ✅ Contract와 문서에서 동일한 샘플 데이터 재사용
- ✅ 중앙 집중식 샘플 데이터 관리

---

### 방안 2: 이벤트 플로우 문서 자동화 개선

#### 현재 상태
- `AvroDocGenerator` Task가 존재 (buildSrc/src/main/kotlin/AvroDocGenerator.kt)
- `docs/generated/event-specifications.md` 자동 생성
- ❌ **event-flows의 README 파일은 수동 관리**

#### 개선 방안: 이벤트 플로우 문서에 자동 생성 섹션 임베딩

##### 1단계: AvroDocGenerator 확장
```kotlin
// buildSrc/src/main/kotlin/AvroDocGenerator.kt 개선
open class AvroDocGenerator : DefaultTask() {

    @TaskAction
    fun generate() {
        // 기존: docs/generated/event-specifications.md 생성
        generateEventSpecifications()

        // 신규: event-flows의 README에 자동 생성 섹션 삽입
        updateEventFlowDocuments()
    }

    private fun updateEventFlowDocuments() {
        // event-flows/**/README.md 파일을 찾아서
        // 특정 마커 사이의 내용을 자동 생성된 명세로 교체

        val eventFlowsDir = project.file("event-flows")
        eventFlowsDir.walkTopDown()
            .filter { it.name == "README.md" }
            .forEach { readmeFile ->
                updateEventSpecSection(readmeFile)
            }
    }

    private fun updateEventSpecSection(readmeFile: File) {
        val content = readmeFile.readText()

        // <!-- AUTO_GENERATED_EVENT_SPEC_START -->
        // ... 자동 생성 내용 ...
        // <!-- AUTO_GENERATED_EVENT_SPEC_END -->
        // 사이의 내용을 자동으로 교체

        val startMarker = "<!-- AUTO_GENERATED_EVENT_SPEC_START -->"
        val endMarker = "<!-- AUTO_GENERATED_EVENT_SPEC_END -->"

        if (content.contains(startMarker) && content.contains(endMarker)) {
            val beforeSection = content.substringBefore(startMarker)
            val afterSection = content.substringAfter(endMarker)

            val generatedSpec = generateEventSpecForDoc(readmeFile)

            val newContent = """
                $beforeSection$startMarker

                $generatedSpec

                $endMarker$afterSection
            """.trimIndent()

            readmeFile.writeText(newContent)
        }
    }

    private fun generateEventSpecForDoc(readmeFile: File): String {
        // Avro 스키마를 읽어서 이벤트 명세를 마크다운으로 생성
        // README에서 참조하는 이벤트들만 필터링

        val events = extractReferencedEvents(readmeFile)

        return events.joinToString("\n\n") { eventName ->
            val avroFile = project.file("src/main/avro/events/${eventName}.avsc")
            if (avroFile.exists()) {
                generateMarkdownSpec(avroFile)
            } else {
                ""
            }
        }
    }
}
```

##### 2단계: 이벤트 플로우 문서 수정
```markdown
<!-- event-flows/order-saga/README.md -->

## 이벤트 명세

> ⚠️ 이 섹션은 자동 생성됩니다. `./gradlew generateAvroEventDocs`를 실행하면 업데이트됩니다.
> 명세를 수정하려면 `src/main/avro/events/*.avsc` 파일을 수정하세요.

<!-- AUTO_GENERATED_EVENT_SPEC_START -->

### 1. OrderCreatedEvent

**발행자**: Order Service
**구독자**: Payment Service
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

**샘플 페이로드**:
```json
{
  "metadata": { ... },
  "orderId": "ORD-12345",
  "customerId": "CUST-001",
  ...
}
```

<!-- AUTO_GENERATED_EVENT_SPEC_END -->

## 상태 전이도
(수동 작성 - 비즈니스 로직)
...
```

**장점**:
- ✅ Avro 스키마만 수정하면 문서 자동 업데이트
- ✅ 수동 작성 부분(다이어그램, 비즈니스 로직)과 자동 생성 부분(명세) 분리
- ✅ 명세 불일치 방지

---

### 방안 3: Avro 스키마 메타데이터 확장

Avro 스키마에 Spring Cloud Contract 및 문서화에 필요한 메타데이터 추가:

```json
// src/main/avro/events/OrderCreatedEvent.avsc
{
  "type": "record",
  "namespace": "com.c4ang.events.order",
  "name": "OrderCreatedEvent",
  "doc": "주문 생성 이벤트 - 주문이 생성되었을 때 발행",

  "metadata": {
    "kafka": {
      "topic": "c4ang.order.created",
      "key": "orderId"
    },
    "producer": "order-service",
    "consumers": ["payment-service"],
    "saga": "order-saga",
    "eventType": "domain",  // domain | compensation | notification
    "retryPolicy": {
      "maxRetries": 3,
      "backoffMs": 1000
    }
  },

  "fields": [...]
}
```

이 메타데이터를 활용하여:
1. Kafka 토픽 자동 추론
2. Producer-Consumer 관계 문서화
3. 재시도 정책 자동 생성
4. Saga 플로우 다이어그램 자동 생성 (일부)

---

## 권장 구현 순서

### Phase 1: 기존 프로젝트 개선 (단기)
1. ✅ **AvroDocGenerator 개선** (이미 존재, 확장 필요)
   - event-flows README 자동 업데이트 기능 추가
   - AUTO_GENERATED 마커 방식으로 안전하게 섹션 교체

2. **이벤트 플로우 문서 리팩토링**
   - 수동 작성된 이벤트 명세를 AUTO_GENERATED 섹션으로 이동
   - 비즈니스 로직, 다이어그램, 정책은 수동 유지

### Phase 2: Spring Cloud Contract 통합 (중기)
3. **Avro 기반 Messaging Contract 작성**
   - 새로운 이벤트부터 Avro 객체 사용
   - 기존 Contract는 점진적으로 마이그레이션

4. **공통 샘플 데이터 생성기 구현**
   - AvroSampleGenerator 구현
   - Contract와 문서에서 공유

### Phase 3: 완전 자동화 (장기)
5. **Avro 스키마 메타데이터 확장**
   - Kafka 토픽, Producer-Consumer 정보 추가

6. **문서 자동 생성 고도화**
   - Saga 플로우 다이어그램 부분 자동화
   - 메트릭, 모니터링 포인트 자동 생성

---

## 최종 워크플로우 (Phase 1 완료 후)

### 이벤트 명세 변경 시
1. **Avro 스키마 수정**: `src/main/avro/events/OrderCreatedEvent.avsc`
2. **빌드 실행**: `./gradlew generateAvroEventDocs`
3. **자동 업데이트**:
   - ✅ Java 클래스 생성 (`build/generated-main-avro-java/`)
   - ✅ 이벤트 명세 문서 (`docs/generated/event-specifications.md`)
   - ✅ 이벤트 플로우 문서 (`event-flows/**/README.md`의 AUTO_GENERATED 섹션)

### 개발자가 수동으로 관리하는 부분
- Mermaid 다이어그램 (비즈니스 플로우)
- 상태 전이도
- 타임아웃/재시도 정책
- 비즈니스 로직 설명
- 보상 트랜잭션 설명

---

## 구현 예시

### 1. AvroDocGenerator 개선 PR
- buildSrc/src/main/kotlin/AvroDocGenerator.kt 수정
- updateEventFlowDocuments() 메서드 추가

### 2. 이벤트 플로우 문서 마이그레이션
- event-flows/order-saga/README.md 수정
- 이벤트 명세 섹션에 AUTO_GENERATED 마커 추가

### 3. 샘플 데이터 생성기 추가
- buildSrc/src/main/kotlin/AvroSampleGenerator.kt 생성

---

## 결론

**현재 관리 포인트**: 3곳 (Avro 스키마, Spring Cloud Contract, 이벤트 문서)

**Phase 1 완료 후**: 1곳 (Avro 스키마만 수정)
- Spring Cloud Contract: 기존 방식 유지 (점진적 마이그레이션)
- 이벤트 문서: 자동 생성 ✅

**Phase 2 완료 후**: 1곳 (Avro 스키마만 수정)
- Spring Cloud Contract: Avro 객체 사용 ✅
- 이벤트 문서: 자동 생성 ✅

→ **관리 포인트 67% 감소, 휴먼 에러 가능성 대폭 감소** 🎉
