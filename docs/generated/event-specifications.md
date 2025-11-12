# 자동 생성된 이벤트 명세

> ⚠️ 이 파일은 자동 생성됩니다. 직접 수정하지 마세요.
> Avro 스키마를 수정한 후 `./gradlew generateAvroEventDocs`를 실행하세요.

## InventoryReservationFailedEvent

**Namespace**: `com.c4ang.events.inventory`

**설명**: 재고 예약 실패 이벤트 - 재고가 부족하여 예약에 실패했을 때 발행 (보상 트랜잭션 트리거)

**Avro 스키마**: `src/main/avro/events/InventoryReservationFailedEvent.avsc`

**Kafka 토픽**: `c4ang.inventory.reservation.failed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `productId` | string | ✅ | 상품 ID |
| `requestedQuantity` | int | ✅ | 요청 수량 |
| `availableQuantity` | int | ✅ | 사용 가능 수량 |
| `reason` | Enum: OUT_OF_STOCK, WAREHOUSE_CLOSED, PRODUCT_DISCONTINUED, UNKNOWN | ✅ | 예약 실패 사유 |

---

## InventoryReservedEvent

**Namespace**: `com.c4ang.events.inventory`

**설명**: 재고 예약 완료 이벤트 - 재고가 성공적으로 예약되었을 때 발행

**Avro 스키마**: `src/main/avro/events/InventoryReservedEvent.avsc`

**Kafka 토픽**: `c4ang.inventory.reserved`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `reservationId` | string | ✅ | 재고 예약 ID |
| `orderId` | string | ✅ | 주문 ID |
| `productId` | string | ✅ | 상품 ID |
| `quantity` | int | ✅ | 예약 수량 |
| `warehouseId` | string | ✅ | 창고 ID |
| `expiresAt` | long | ✅ | 예약 만료 시각 |

---

## OrderCreatedEvent

**Namespace**: `com.c4ang.events.order`

**설명**: 주문 생성 이벤트 - 주문이 생성되었을 때 발행

**Avro 스키마**: `src/main/avro/events/OrderCreatedEvent.avsc`

**Kafka 토픽**: `c4ang.order.created`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `customerId` | string | ✅ | 고객 ID |
| `productId` | string | ✅ | 상품 ID |
| `quantity` | int | ✅ | 주문 수량 |
| `totalAmount` | Decimal | ✅ | 주문 총액 |
| `orderStatus` | Enum: PENDING_PAYMENT, PAYMENT_COMPLETED, PAYMENT_FAILED, INVENTORY_RESERVED, OUT_OF_STOCK, COMPLETED, CANCELLED | ✅ | 주문 상태 |

---

## PaymentCancelledEvent

**Namespace**: `com.c4ang.events.payment`

**설명**: 결제 취소 이벤트 - 결제가 취소되고 환불이 처리될 때 발행 (보상 트랜잭션)

**Avro 스키마**: `src/main/avro/events/PaymentCancelledEvent.avsc`

**Kafka 토픽**: `c4ang.payment.cancelled`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `paymentId` | string | ✅ | 결제 ID |
| `orderId` | string | ✅ | 주문 ID |
| `refundAmount` | Decimal | ✅ | 환불 금액 |
| `reason` | string | ✅ | 취소 사유 |
| `refundTransactionId` | string | ❌ | 환불 거래 ID (선택) |

---

## PaymentCompletedEvent

**Namespace**: `com.c4ang.events.payment`

**설명**: 결제 완료 이벤트 - 결제가 성공적으로 완료되었을 때 발행

**Avro 스키마**: `src/main/avro/events/PaymentCompletedEvent.avsc`

**Kafka 토픽**: `c4ang.payment.completed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `paymentId` | string | ✅ | 결제 ID |
| `orderId` | string | ✅ | 주문 ID |
| `amount` | Decimal | ✅ | 결제 금액 |
| `paymentMethod` | Enum: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, MOBILE_PAY, POINT | ✅ | 결제 수단 |
| `transactionId` | string | ❌ | PG사 거래 ID (선택) |

---

## PaymentFailedEvent

**Namespace**: `com.c4ang.events.payment`

**설명**: 결제 실패 이벤트 - 결제가 실패했을 때 발행 (보상 트랜잭션 트리거)

**Avro 스키마**: `src/main/avro/events/PaymentFailedEvent.avsc`

**Kafka 토픽**: `c4ang.payment.failed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `metadata` | com.c4ang.events.common.EventMetadata | ✅ | 이벤트 메타데이터 |
| `orderId` | string | ✅ | 주문 ID |
| `reason` | Enum: INSUFFICIENT_BALANCE, CARD_DECLINED, INVALID_CARD, NETWORK_ERROR, TIMEOUT, FRAUD_DETECTED, UNKNOWN | ✅ | 결제 실패 사유 |
| `failureMessage` | string | ✅ | 실패 상세 메시지 |
| `retryable` | boolean | ✅ | 재시도 가능 여부 |

---

