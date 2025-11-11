# Contract Test 문서

## 개요

이 디렉토리는 Spring Cloud Contract를 사용한 Kafka 이벤트 및 HTTP API의 Contract Test를 포함합니다.
kafka-event-sequence.md 문서의 이벤트 흐름을 기반으로 작성되었습니다.

## 디렉토리 구조

```
contracts/
├── store-service/          # Store 도메인 Contract
│   └── store_deleted_event.kts
├── order-service/          # Order 도메인 Contract
│   ├── order_created_event.kts
│   ├── order_confirmed_event.kts
│   ├── order_cancelled_event.kts
│   ├── order_expiration_notification_event.kts
│   └── stock_confirmed_event.kts
├── product-service/        # Product 도메인 Contract
│   └── stock_reserved_event.kts
├── payment-service/        # Payment 도메인 Contract
│   ├── payment_completed_event.kts
│   ├── payment_failed_event.kts
│   └── payment_cancelled_event.kts
└── saga/                   # SAGA 패턴 Contract
    ├── stock_reservation_failed_event.kts
    ├── stock_confirmation_failed_event.kts
    ├── order_confirmation_compensate_event.kts
    ├── payment_completion_compensate_event.kts
    └── saga_tracker_event.kts
```

## SAGA 패턴 이벤트 흐름

### 1. Order Creation Saga (정상 흐름)

```
1. order.created (Order Service)
   → Product Service 소비
2. stock.reserved (Product Service)
   → Order Service 소비
3. order.confirmed (Order Service)
   → Payment Service 소비
4. 결제 대기 생성 완료
```

### 2. Order Creation Saga (재고 부족 실패)

```
1. order.created (Order Service)
   → Product Service 소비
2. saga.stock-reservation.failed (Product Service)
   → Order Service 소비
3. 보상 트랜잭션: 주문 취소 (ORDER_CANCELLED)
```

### 3. Payment Saga (정상 흐름)

```
1. payment.completed (Payment Service)
   → Order Service 소비
2. order.stock.confirmed (Order Service)
   → Payment Service 소비
3. Saga 완료
```

### 4. Payment Saga (재고 확정 실패)

```
1. payment.completed (Payment Service)
   → Order Service 소비
2. saga.stock-confirmation.failed (Order Service)
   → Payment Service 소비
3. saga.payment-completion.compensate (Payment Service)
   → Order Service 소비
4. saga.order-confirmation.compensate (Order Service)
   → Product Service 소비
5. 재고 복원 완료
```

### 5. Payment Saga (결제 실패)

```
1. payment.failed (Payment Service)
   → Order Service 소비
2. order.cancelled (Order Service)
   → Product Service 소비
3. 재고 복원 완료
```

## Contract 작성 규칙

### Kafka 이벤트 Contract

```kotlin
contract {
    description = "이벤트 설명"

    input {
        triggeredBy("triggerMethodName()")
    }

    outputMessage {
        sentTo("topic.name")

        body("""
            {
                "eventId": "...",
                "eventTimestamp": ...,
                ...
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "partition-key-value")
        }
    }
}
```

### 주요 포인트

1. **파티션 키**: orderId 또는 sagaId를 파티션 키로 사용하여 순서 보장
2. **멱등성**: eventId로 중복 처리 방지
3. **Avro 스키마**: 모든 이벤트는 src/main/avro 아래 정의된 Avro 스키마 기반
4. **타임스탬프**: timestamp-millis logicalType 사용 (Epoch milliseconds)

## 테스트 실행

```bash
# Contract 테스트 실행
./gradlew contractTest

# Producer Stub 생성
./gradlew publishStubsToScm

# Consumer Stub 다운로드 및 테스트
./gradlew copyContracts
./gradlew test
```

## 참고 문서

- kafka-event-sequence.md: 이벤트 흐름 시퀀스 다이어그램
- kafka-event-specifications.md: 이벤트 명세 및 SAGA 패턴 상세
- src/main/avro/: Avro 스키마 정의

## 문의

문의사항은 #kafka-events 채널로 연락 주세요.
