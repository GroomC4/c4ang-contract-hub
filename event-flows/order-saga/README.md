# 주문 Saga 플로우

## 개요

주문 생성부터 완료까지의 Choreography Saga 패턴 이벤트 흐름을 정의합니다.

## 정상 플로우

```mermaid
sequenceDiagram
    participant OrderService
    participant PaymentService
    participant InventoryService
    participant NotificationService

    OrderService->>PaymentService: OrderCreatedEvent
    Note right of OrderService: 주문 생성<br/>상태: PENDING_PAYMENT

    PaymentService->>InventoryService: PaymentCompletedEvent
    Note right of PaymentService: 결제 완료<br/>결제 금액 차감

    InventoryService->>NotificationService: InventoryReservedEvent
    Note right of InventoryService: 재고 예약<br/>재고 수량 차감

    NotificationService->>OrderService: OrderCompletedNotificationSentEvent
    Note right of NotificationService: 알림 발송<br/>고객에게 완료 알림

    OrderService->>OrderService: Update Order Status
    Note right of OrderService: 주문 완료<br/>상태: COMPLETED
```

## 실패 시나리오 1: 결제 실패

```mermaid
sequenceDiagram
    participant OrderService
    participant PaymentService

    OrderService->>PaymentService: OrderCreatedEvent
    Note right of OrderService: 주문 생성<br/>상태: PENDING_PAYMENT

    PaymentService->>OrderService: PaymentFailedEvent
    Note right of PaymentService: 결제 실패<br/>잔액 부족 등

    OrderService->>OrderService: Compensate Order
    Note right of OrderService: 보상 트랜잭션<br/>상태: PAYMENT_FAILED
```

## 실패 시나리오 2: 재고 부족

```mermaid
sequenceDiagram
    participant OrderService
    participant PaymentService
    participant InventoryService

    OrderService->>PaymentService: OrderCreatedEvent
    PaymentService->>InventoryService: PaymentCompletedEvent

    InventoryService->>PaymentService: InventoryReservationFailedEvent
    Note right of InventoryService: 재고 부족

    PaymentService->>OrderService: PaymentCancelledEvent
    Note right of PaymentService: 결제 취소<br/>결제 금액 환불

    OrderService->>OrderService: Compensate Order
    Note right of OrderService: 보상 트랜잭션<br/>상태: OUT_OF_STOCK
```

## 이벤트 명세

이 섹션의 이벤트 필드 정보는 Avro 스키마로부터 자동 생성됩니다.
**발행자/구독자 정보**와 **트리거 조건, 비즈니스 로직**은 수동으로 관리합니다.

<!-- AUTO_GENERATED_EVENT_SPEC_START -->

> ⚠️ 이 섹션은 자동 생성됩니다. `./gradlew generateAvroEventDocs`를 실행하면 업데이트됩니다.
> 명세를 수정하려면 `src/main/avro/events/*.avsc` 파일을 수정하세요.

### 1. OrderCreatedEvent

**Kafka 토픽**: `c4ang.order.created`

**Avro 스키마**: `src/main/avro/events/OrderCreatedEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `customerId` | string | ✅ | 고객 ID |
| `productId` | string | ✅ | 상품 ID |
| `quantity` | int | ✅ | 주문 수량 |
| `totalAmount` | Decimal | ✅ | 주문 총액 |
| `orderStatus` | Enum: PENDING_PAYMENT, PAYMENT_COMPLETED, PAYMENT_FAILED, INVENTORY_RESERVED, OUT_OF_STOCK, COMPLETED, CANCELLED | ✅ | 주문 상태 |

### 2. PaymentCompletedEvent

**Kafka 토픽**: `c4ang.payment.completed`

**Avro 스키마**: `src/main/avro/events/PaymentCompletedEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `paymentId` | string | ✅ | 결제 ID |
| `orderId` | string | ✅ | 주문 ID |
| `amount` | Decimal | ✅ | 결제 금액 |
| `paymentMethod` | Enum: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, MOBILE_PAY, POINT | ✅ | 결제 수단 |
| `transactionId` | string | ❌ | PG사 거래 ID (선택) |

### 3. InventoryReservedEvent

**Kafka 토픽**: `c4ang.inventory.reserved`

**Avro 스키마**: `src/main/avro/events/InventoryReservedEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `reservationId` | string | ✅ | 재고 예약 ID |
| `orderId` | string | ✅ | 주문 ID |
| `productId` | string | ✅ | 상품 ID |
| `quantity` | int | ✅ | 예약 수량 |
| `warehouseId` | string | ✅ | 창고 ID |
| `expiresAt` | long | ✅ | 예약 만료 시각 |

### 4. PaymentFailedEvent

**Kafka 토픽**: `c4ang.payment.failed`

**Avro 스키마**: `src/main/avro/events/PaymentFailedEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `reason` | Enum: INSUFFICIENT_BALANCE, CARD_DECLINED, INVALID_CARD, NETWORK_ERROR, TIMEOUT, FRAUD_DETECTED, UNKNOWN | ✅ | 결제 실패 사유 |
| `failureMessage` | string | ✅ | 실패 상세 메시지 |
| `retryable` | boolean | ✅ | 재시도 가능 여부 |

### 5. InventoryReservationFailedEvent

**Kafka 토픽**: `c4ang.inventory.reservation.failed`

**Avro 스키마**: `src/main/avro/events/InventoryReservationFailedEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `productId` | string | ✅ | 상품 ID |
| `requestedQuantity` | int | ✅ | 요청 수량 |
| `availableQuantity` | int | ✅ | 사용 가능 수량 |
| `reason` | Enum: OUT_OF_STOCK, WAREHOUSE_CLOSED, PRODUCT_DISCONTINUED, UNKNOWN | ✅ | 예약 실패 사유 |

### 6. PaymentCancelledEvent

**Kafka 토픽**: `c4ang.payment.cancelled`

**Avro 스키마**: `src/main/avro/events/PaymentCancelledEvent.avsc`

**필드**:

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `paymentId` | string | ✅ | 결제 ID |
| `orderId` | string | ✅ | 주문 ID |
| `refundAmount` | Decimal | ✅ | 환불 금액 |
| `reason` | string | ✅ | 취소 사유 |
| `refundTransactionId` | string | ❌ | 환불 거래 ID (선택) |

<!-- AUTO_GENERATED_EVENT_SPEC_END -->

### 이벤트별 추가 정보

#### 1. OrderCreatedEvent
**발행자**: Order Service
**구독자**: Payment Service
**트리거 조건**: 고객이 주문을 생성할 때
**비즈니스 로직**: 주문 정보를 DB에 저장하고 결제 대기 상태로 설정

#### 2. PaymentCompletedEvent
**발행자**: Payment Service
**구독자**: Inventory Service
**트리거 조건**: 결제가 성공적으로 완료되었을 때
**비즈니스 로직**: 결제 금액 차감 및 결제 내역 저장

#### 3. PaymentFailedEvent (보상)
**발행자**: Payment Service
**구독자**: Order Service
**트리거 조건**: 결제 처리 중 오류 발생 시
**비즈니스 로직**: 주문 상태를 PAYMENT_FAILED로 업데이트

#### 4. InventoryReservedEvent
**발행자**: Inventory Service
**구독자**: Notification Service
**트리거 조건**: 재고 예약이 성공했을 때
**비즈니스 로직**: 재고 수량 차감 및 예약 정보 저장

#### 5. InventoryReservationFailedEvent (보상)
**발행자**: Inventory Service
**구독자**: Payment Service
**트리거 조건**: 재고 부족으로 예약 실패 시
**비즈니스 로직**: 결제 취소 및 환불 트리거

#### 6. PaymentCancelledEvent (보상)
**발행자**: Payment Service
**구독자**: Order Service
**트리거 조건**: 재고 예약 실패로 결제 취소가 필요할 때
**비즈니스 로직**: 결제 금액 환불 및 주문 상태를 OUT_OF_STOCK으로 업데이트

## 상태 전이도

```
PENDING_PAYMENT (초기)
    ↓
    ├─→ PAYMENT_FAILED (결제 실패)
    └─→ PAYMENT_COMPLETED
            ↓
            ├─→ OUT_OF_STOCK (재고 부족)
            └─→ INVENTORY_RESERVED
                    ↓
                    └─→ COMPLETED (완료)
```

## 타임아웃 정책

- **결제 대기**: 5분 (이후 자동 취소)
- **재고 예약 대기**: 3분 (이후 결제 환불 및 주문 취소)
- **전체 Saga 타임아웃**: 10분

## 재시도 정책

- **결제 실패**: 재시도 없음 (즉시 실패 처리)
- **재고 예약 실패**: 재시도 없음 (결제 환불 및 주문 취소)
- **알림 발송 실패**: 최대 3회 재시도 (1분 간격)

## 멱등성 보장

모든 이벤트 핸들러는 `eventId`를 기반으로 중복 처리를 방지합니다.

## 모니터링 포인트

- 각 단계별 처리 시간
- 보상 트랜잭션 발생 빈도
- 실패 원인별 통계
- Saga 완료율
