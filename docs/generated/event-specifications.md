# 자동 생성된 이벤트 명세

> ⚠️ 이 파일은 자동 생성됩니다. 직접 수정하지 마세요.
> Avro 스키마를 수정한 후 `./gradlew generateAvroEventDocs`를 실행하세요.

## DailyStatistics

**Namespace**: `com.groom.ecommerce.analytics.event.avro`

**설명**: 일일 통계 리포트 이벤트 - Analytics/Notification Service가 처리

**Avro 스키마**: `src/main/events/avro/DailyStatistics.avsc`

**Kafka 토픽**: `c4ang.analytics.daily-statistics`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `date` | string | ✅ | 통계 날짜 (YYYY-MM-DD) |
| `totalOrders` | int | ✅ | 총 주문 건수 |
| `totalSales` | Decimal | ✅ | 총 매출액 |
| `avgOrderAmount` | Decimal | ✅ | 평균 주문 금액 |
| `topProducts` | array | ✅ | 인기 상품 Top 10 |
| `generatedAt` | long | ✅ | - |

---

## EventMetadata

**Namespace**: `com.c4ang.events.common`

**설명**: 이벤트 메타데이터 - 모든 이벤트에 공통으로 포함되는 정보

**Avro 스키마**: `src/main/events/avro/EventMetadata.avsc`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | 이벤트 고유 ID (UUID) |
| `eventType` | string | ✅ | 이벤트 타입 (예: OrderCreated, PaymentCompleted) |
| `timestamp` | long | ✅ | 이벤트 발생 시각 (Epoch milliseconds) |
| `correlationId` | string | ✅ | 비즈니스 상관 ID (예: 주문 ID, 결제 ID) |
| `version` | string | ✅ | 이벤트 스키마 버전 |
| `source` | string | ✅ | 이벤트 발행 서비스 이름 |

---

## OrderCancelled

**Namespace**: `com.groom.ecommerce.order.event.avro`

**설명**: 주문 취소 이벤트 - Product Service가 재고 복원 처리

**Avro 스키마**: `src/main/events/avro/OrderCancelled.avsc`

**Kafka 토픽**: `c4ang.order.cancelled`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | 이벤트 고유 ID (UUID) |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | 취소된 주문 ID |
| `userId` | string | ✅ | - |
| `items` | array | ✅ | - |
| `cancellationReason` | Enum: PAYMENT_TIMEOUT, USER_REQUESTED, STOCK_UNAVAILABLE, SYSTEM_ERROR | ✅ | 취소 사유 |
| `cancelledAt` | long | ✅ | - |

---

## OrderConfirmationCompensate

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 주문 확정 보상 이벤트 - 결제 실패/초기화 실패 시 재고 복원

**Avro 스키마**: `src/main/events/avro/OrderConfirmationCompensate.avsc`

**Kafka 토픽**: `c4ang.order.confirmation-compensate`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `items` | array | ✅ | - |
| `compensationReason` | string | ✅ | 결제 실패, 결제 대기 생성 실패 등 |
| `compensatedAt` | long | ✅ | - |

---

## OrderConfirmed

**Namespace**: `com.groom.ecommerce.order.event.avro`

**설명**: 주문 확정 이벤트 - Payment Service가 결제 대기 생성

**Avro 스키마**: `src/main/events/avro/OrderConfirmed.avsc`

**Kafka 토픽**: `c4ang.order.confirmed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `userId` | string | ✅ | - |
| `totalAmount` | Decimal | ✅ | - |
| `confirmedAt` | long | ✅ | - |

---

## OrderCreated

**Namespace**: `com.groom.ecommerce.order.event.avro`

**설명**: 주문 생성 이벤트 - Product Service가 재고 예약 처리

**Avro 스키마**: `src/main/events/avro/OrderCreated.avsc`

**Kafka 토픽**: `c4ang.order.created`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | 이벤트 고유 ID (UUID) - 멱등성 보장 |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | 주문 ID (파티션 키) |
| `userId` | string | ✅ | - |
| `storeId` | string | ✅ | - |
| `items` | array | ✅ | - |
| `totalAmount` | Decimal | ✅ | - |
| `createdAt` | long | ✅ | - |

---

## OrderCreationCompensate

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 주문 생성 보상 이벤트 - 재고 예약 실패 시 주문 취소

**Avro 스키마**: `src/main/events/avro/OrderCreationCompensate.avsc`

**Kafka 토픽**: `c4ang.order.creation-compensate`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `compensationReason` | string | ✅ | 보상 트랜잭션 사유 |
| `compensatedAt` | long | ✅ | - |

---

## OrderExpirationNotification

**Namespace**: `com.groom.ecommerce.order.event.avro`

**설명**: 주문 만료 알림 이벤트 - Notification Service가 고객 알림 발송

**Avro 스키마**: `src/main/events/avro/OrderExpirationNotification.avsc`

**Kafka 토픽**: `c4ang.order.expiration-notification`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `userId` | string | ✅ | 알림 수신 대상 |
| `expirationReason` | string | ✅ | 만료 사유 |
| `expiredAt` | long | ✅ | - |

---

## PaymentCancelled

**Namespace**: `com.groom.ecommerce.payment.event.avro`

**설명**: 결제 취소 이벤트 - Order Service가 주문 상태 업데이트

**Avro 스키마**: `src/main/events/avro/PaymentCancelled.avsc`

**Kafka 토픽**: `c4ang.payment.cancelled`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `paymentId` | string | ✅ | - |
| `orderId` | string | ✅ | 취소된 주문 ID |
| `userId` | string | ✅ | - |
| `cancellationReason` | Enum: STOCK_UNAVAILABLE, ADMIN_CANCEL, USER_CANCEL, SYSTEM_ERROR | ✅ | 취소 사유 |
| `cancelledAt` | long | ✅ | - |

---

## PaymentCompleted

**Namespace**: `com.groom.ecommerce.payment.event.avro`

**설명**: 결제 완료 이벤트 - Order Service가 재고 확정 처리

**Avro 스키마**: `src/main/events/avro/PaymentCompleted.avsc`

**Kafka 토픽**: `c4ang.payment.completed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `paymentId` | string | ✅ | - |
| `orderId` | string | ✅ | 파티션 키로 사용 (순서 보장) |
| `userId` | string | ✅ | - |
| `totalAmount` | Decimal | ✅ | - |
| `paymentMethod` | Enum: CARD, BANK_TRANSFER, KAKAO_PAY, NAVER_PAY, TOSS | ✅ | - |
| `pgApprovalNumber` | string | ✅ | - |
| `completedAt` | long | ✅ | - |

---

## PaymentCompletionCompensate

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 결제 완료 보상 이벤트 - 재고 확정 실패 시 결제 취소

**Avro 스키마**: `src/main/events/avro/PaymentCompletionCompensate.avsc`

**Kafka 토픽**: `c4ang.payment.completion-compensate`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `paymentId` | string | ✅ | - |
| `orderId` | string | ✅ | - |
| `refundAmount` | Decimal | ✅ | 환불 금액 |
| `compensationReason` | string | ✅ | 재고 확정 실패 |
| `compensatedAt` | long | ✅ | - |

---

## PaymentFailed

**Namespace**: `com.groom.ecommerce.payment.event.avro`

**설명**: 결제 실패 이벤트 - Order Service가 보상 트랜잭션 실행

**Avro 스키마**: `src/main/events/avro/PaymentFailed.avsc`

**Kafka 토픽**: `c4ang.payment.failed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `paymentId` | string | ✅ | - |
| `orderId` | string | ✅ | - |
| `userId` | string | ✅ | - |
| `failureReason` | string | ✅ | - |
| `failedAt` | long | ✅ | - |

---

## PaymentInitializationFailed

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 결제 대기 생성 실패 이벤트 - Order Service가 보상 트랜잭션 실행

**Avro 스키마**: `src/main/events/avro/PaymentInitializationFailed.avsc`

**Kafka 토픽**: `c4ang.payment.initialization-failed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `failureReason` | string | ✅ | - |
| `failedAt` | long | ✅ | - |

---

## SagaTracker

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 흐름 추적 및 감사 이벤트 - 모든 Saga 단계 기록

**Avro 스키마**: `src/main/events/avro/SagaTracker.avsc`

**Kafka 토픽**: `c4ang.saga.tracker`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `sagaId` | string | ✅ | Saga 고유 ID (주문 ID 또는 별도 Saga ID) |
| `sagaType` | Enum: ORDER_CREATION, PAYMENT_COMPLETION | ✅ | Saga 유형 |
| `step` | string | ✅ | 현재 Saga 단계 (예: STOCK_RESERVATION, PAYMENT_INITIALIZATION) |
| `status` | Enum: STARTED, IN_PROGRESS, COMPLETED, FAILED, COMPENSATED | ✅ | Saga 상태 |
| `orderId` | string | ✅ | - |
| `metadata` | string | ❌ | 추가 메타데이터 (JSON) |
| `recordedAt` | long | ✅ | - |

---

## StockConfirmationCompensate

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 재고 확정 보상 이벤트 - 재고 복원 처리

**Avro 스키마**: `src/main/events/avro/StockConfirmationCompensate.avsc`

**Kafka 토픽**: `c4ang.stock.confirmation-compensate`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `paymentId` | string | ✅ | - |
| `items` | array | ✅ | - |
| `compensationReason` | string | ✅ | - |
| `compensatedAt` | long | ✅ | - |

---

## StockConfirmationFailed

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 재고 확정 실패 이벤트 - Payment Service가 보상 트랜잭션 실행

**Avro 스키마**: `src/main/events/avro/StockConfirmationFailed.avsc`

**Kafka 토픽**: `c4ang.stock.confirmation-failed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `paymentId` | string | ✅ | - |
| `failureReason` | string | ✅ | - |
| `failedAt` | long | ✅ | - |

---

## StockConfirmed

**Namespace**: `com.groom.ecommerce.order.event.avro`

**설명**: 재고 확정 성공 이벤트 - Payment Saga 완료

**Avro 스키마**: `src/main/events/avro/StockConfirmed.avsc`

**Kafka 토픽**: `c4ang.stock.confirmed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `paymentId` | string | ✅ | - |
| `confirmedItems` | array | ✅ | - |
| `confirmedAt` | long | ✅ | - |

---

## StockReservationCompensate

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 재고 예약 보상 이벤트 - 재고 복원 처리

**Avro 스키마**: `src/main/events/avro/StockReservationCompensate.avsc`

**Kafka 토픽**: `c4ang.stock.reservation-compensate`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `items` | array | ✅ | - |
| `compensationReason` | string | ✅ | - |
| `compensatedAt` | long | ✅ | - |

---

## StockReservationFailed

**Namespace**: `com.groom.ecommerce.saga.event.avro`

**설명**: SAGA 재고 예약 실패 이벤트 - Order Service가 보상 트랜잭션 실행

**Avro 스키마**: `src/main/events/avro/StockReservationFailed.avsc`

**Kafka 토픽**: `c4ang.stock.reservation-failed`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `failedItems` | array | ✅ | - |
| `failureReason` | string | ✅ | 재고 부족, 상품 미존재 등 |
| `failedAt` | long | ✅ | - |

---

## StockReserved

**Namespace**: `com.groom.ecommerce.product.event.avro`

**설명**: 재고 예약 성공 이벤트 - Order Service가 주문 확정 처리

**Avro 스키마**: `src/main/events/avro/StockReserved.avsc`

**Kafka 토픽**: `c4ang.stock.reserved`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `orderId` | string | ✅ | - |
| `reservedItems` | array | ✅ | - |
| `reservedAt` | long | ✅ | - |

---

## StockSyncAlert

**Namespace**: `com.groom.ecommerce.monitoring.event.avro`

**설명**: 재고 불일치 알림 이벤트 - Notification Service가 Slack/PagerDuty 알림 발송

**Avro 스키마**: `src/main/events/avro/StockSyncAlert.avsc`

**Kafka 토픽**: `c4ang.stock.sync-alert`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `productId` | string | ✅ | - |
| `dbStock` | int | ✅ | DB 재고 (신뢰 데이터) |
| `redisStock` | int | ✅ | Redis 재고 (불일치 발견) |
| `discrepancy` | int | ✅ | 불일치 수량 (dbStock - redisStock) |
| `action` | Enum: REDIS_RESTORED, MANUAL_REVIEW_REQUIRED | ✅ | 조치 내용 |
| `severity` | Enum: LOW, MEDIUM, HIGH, CRITICAL | ✅ | - |
| `detectedAt` | long | ✅ | - |

---

## StoreDeleted

**Namespace**: `com.groom.ecommerce.store.event.avro`

**설명**: 스토어 삭제 이벤트 - Product Service가 연관 상품 처리

**Avro 스키마**: `src/main/events/avro/StoreDeleted.avsc`

**Kafka 토픽**: `c4ang.store.deleted`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | - |
| `eventTimestamp` | long | ✅ | - |
| `storeId` | string | ✅ | 삭제된 스토어 ID |
| `ownerId` | string | ✅ | - |
| `deletedAt` | long | ✅ | - |

---

## StoreInfoUpdated

**Namespace**: `com.groom.ecommerce.store.event.avro`

**설명**: 스토어 정보 변경 이벤트 - Product Service가 연관 상품의 비정규화된 스토어 정보 업데이트

**Avro 스키마**: `src/main/events/avro/StoreInfoUpdated.avsc`

**Kafka 토픽**: `c4ang.store.info-updated`

### 필드

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| `eventId` | string | ✅ | 이벤트 고유 ID (UUID) - 멱등성 보장 |
| `eventTimestamp` | long | ✅ | - |
| `storeId` | string | ✅ | 변경된 스토어 ID (파티션 키) |
| `storeName` | string | ✅ | 스토어 이름 |
| `storeStatus` | string | ✅ | 스토어 상태 (ACTIVE, INACTIVE, SUSPENDED, DELETED) |
| `storeDescription` | string | ❌ | 스토어 설명 |
| `storePhone` | string | ❌ | 스토어 전화번호 |
| `storeAddress` | {type=record, name=Address, fields=[{name=street, type=string}, {name=city, type=string}, {name=state, type=string}, {name=zipCode, type=string}, {name=country, type=string}]} | ❌ | 스토어 주소 |
| `businessHours` | string | ❌ | 영업 시간 정보 (JSON 형식) |
| `storeImageUrl` | string | ❌ | 스토어 이미지 URL |
| `updatedFields` | array | ✅ | 변경된 필드 목록 (예: ['storeName', 'storePhone']) |
| `updatedAt` | long | ✅ | - |

---

