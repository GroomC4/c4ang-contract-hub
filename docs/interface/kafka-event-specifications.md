# Kafka 이벤트 명세서

## 문서 개요

**작성일**: 2025-11-11 (최종 수정)
**버전**: 2.0
**대상 독자**: Kafka 인프라 담당자, Spring Cloud Contract 담당자, 백엔드 개발자

**목적**:
- Kafka 토픽 생성 및 설정을 위한 명세 제공
- Spring Cloud Contract 기반 이벤트 테스트 구현 가이드
- Producer/Consumer 간 계약(Contract) 정의

**주요 변경사항 (v2.0)**:
- SAGA 패턴 보상 트랜잭션 토픽 네이밍 규칙 적용 (`saga.*`)
- 비즈니스 이벤트와 SAGA 이벤트 명확히 구분
- Saga Tracker 토픽 추가

**참고 문서**:
- `카프카 네이밍 규칙(with.SAGA패턴).md` - 네이밍 규칙 상세
- `카프카+SAGA패턴 토픽 네이밍 전략.md` - 설계 배경 및 논의

---

## 📋 목차

1. [Kafka 토픽 설정](#1-kafka-토픽-설정)
2. [이벤트 스키마 명세 (Avro)](#2-이벤트-스키마-명세-avro)
3. [Spring Cloud Contract 명세](#3-spring-cloud-contract-명세)
4. [동기 HTTP API 명세](#4-동기-http-api-명세)
5. [구현 가이드](#5-구현-가이드)

---

## 1. Kafka 토픽 설정

### 1.1 토픽 생성 스크립트

**Kafka 담당자**: 아래 설정으로 토픽을 생성해주세요.

```bash
#!/bin/bash
# create-topics.sh

KAFKA_BROKER="localhost:9092"
REPLICATION_FACTOR=3

# ==========================================
# 비즈니스 이벤트 토픽 (Business Events)
# ==========================================

# Store 도메인
kafka-topics.sh --create --topic store.deleted \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

# Order 도메인 (Forward Events)
kafka-topics.sh --create --topic order.created \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic order.confirmed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic order.cancelled \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic order.expiration.notification \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic order.stock.confirmed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

# Product 도메인 (재고)
kafka-topics.sh --create --topic stock.reserved \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

# Payment 도메인 (금융 데이터, 장기 보관)
kafka-topics.sh --create --topic payment.completed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000 \
  --config compression.type=snappy

kafka-topics.sh --create --topic payment.failed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000

kafka-topics.sh --create --topic payment.refunded \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000

# ==========================================
# SAGA 패턴 이벤트 토픽 (SAGA Events)
# ==========================================

# SAGA 실패 이벤트 (Failure Events)
kafka-topics.sh --create --topic saga.stock-reservation.failed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic saga.payment-initialization.failed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic saga.stock-confirmation.failed \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

# SAGA 보상 이벤트 (Compensation Events)
kafka-topics.sh --create --topic saga.order-creation.compensate \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic saga.order-confirmation.compensate \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic saga.payment-completion.compensate \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic saga.stock-reservation.compensate \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic saga.stock-confirmation.compensate \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

# SAGA Tracker (단일 토픽, Payload로 구분)
kafka-topics.sh --create --topic saga.tracker \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000

# ==========================================
# 모니터링 및 분석 토픽
# ==========================================

kafka-topics.sh --create --topic stock.sync.alert \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 1 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=604800000

kafka-topics.sh --create --topic analytics.daily.statistics \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 1 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000

# ==========================================
# Dead Letter Topics
# ==========================================

kafka-topics.sh --create --topic product.created.dlt \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 1 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000

kafka-topics.sh --create --topic payment.completed.dlt \
  --bootstrap-server $KAFKA_BROKER \
  --partitions 1 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=2592000000
```

---

### 1.2 토픽 설정 요약표

#### 비즈니스 이벤트 (Business Events)

| 토픽명 | 이벤트 유형 | 파티션 | Replication | Retention | 파티션 키 | 용도 |
|--------|-----------|--------|-------------|-----------|----------|------|
| `store.deleted` | Business | 3 | 3 | 7일 | `storeId` | Cascade Operation |
| `order.created` | Forward | 3 | 3 | 7일 | `orderId` | Order Creation Saga 시작 |
| `stock.reserved` | Forward | 3 | 3 | 7일 | `orderId` | 재고 예약 성공 |
| `order.confirmed` | Forward | 3 | 3 | 7일 | `orderId` | 주문 확정 성공 |
| `order.cancelled` | Business | 3 | 3 | 7일 | `orderId` | 사용자/관리자 주문 취소 |
| `order.expiration.notification` | Business | 3 | 3 | 7일 | `orderId` | 주문 만료 알림 |
| `order.stock.confirmed` | Forward | 3 | 3 | 7일 | `orderId` | 재고 확정 성공 (Payment Saga) |
| `payment.completed` | Forward | 3 | 3 | 30일 | `orderId` | 결제 완료 (금융 데이터, Snappy 압축) |
| `payment.failed` | Forward | 3 | 3 | 30일 | `orderId` | 결제 실패 (금융 데이터) |
| `payment.refunded` | Business | 3 | 3 | 30일 | `orderId` | 사용자 환불 요청 (금융 데이터) |
| `stock.sync.alert` | Monitoring | 1 | 3 | 7일 | `productId` | 재고 불일치 알림 |
| `analytics.daily.statistics` | Analytics | 1 | 3 | 30일 | `date` | 일일 통계 리포트 |

#### SAGA 패턴 이벤트 (SAGA Events)

| 토픽명 | 이벤트 유형 | 파티션 | Replication | Retention | 파티션 키 | 용도 |
|--------|-----------|--------|-------------|-----------|----------|------|
| `saga.stock-reservation.failed` | **SAGA Failure** | 3 | 3 | 7일 | `orderId` | **재고 예약 실패 알림** |
| `saga.payment-initialization.failed` | **SAGA Failure** | 3 | 3 | 7일 | `orderId` | **결제 대기 생성 실패 알림** |
| `saga.stock-confirmation.failed` | **SAGA Failure** | 3 | 3 | 7일 | `orderId` | **재고 확정 실패 알림** |
| `saga.order-creation.compensate` | **SAGA Compensation** | 3 | 3 | 7일 | `orderId` | **주문 생성 보상** |
| `saga.order-confirmation.compensate` | **SAGA Compensation** | 3 | 3 | 7일 | `orderId` | **주문 확정 보상** |
| `saga.payment-completion.compensate` | **SAGA Compensation** | 3 | 3 | 7일 | `orderId` | **결제 완료 보상** |
| `saga.stock-reservation.compensate` | **SAGA Compensation** | 3 | 3 | 7일 | `orderId` | **재고 예약 보상** |
| `saga.stock-confirmation.compensate` | **SAGA Compensation** | 3 | 3 | 7일 | `orderId` | **재고 확정 보상** |
| `saga.tracker` | **SAGA Tracker** | 3 | 3 | 30일 | `sagaId` | **Saga 흐름 추적 및 감사** |

**Retention 정책**:
- 7일 (604800000ms): 일반 이벤트 (장애 복구 시간 고려)
- 30일 (2592000000ms): 금융/감사 데이터 (규정 준수)

**파티션 수 산정 기준**:

파티션 수는 다음 공식으로 계산합니다:
```
필요 파티션 수 = ⌈ 목표 TPS / Consumer당 처리 가능 TPS ⌉
```

**초기 설정 (보수적 접근)**:
- **3개**: 대부분의 비즈니스 토픽 (Product, Store, Order, Payment)
  - 이유: 초기 서비스는 낮은 TPS로 시작 (예상 TPS: 100~300 msg/sec)
  - Consumer당 처리 속도: 100 msg/sec 가정 시 3개 파티션으로 300 msg/sec 처리 가능
  - Order Saga 토픽의 경우 `orderId` 파티션 키 사용으로 동일 주문은 순서 보장
- **1개**: Dead Letter Topics (DLT)
  - 이유: 에러 메시지는 낮은 빈도 (예상 TPS: < 10 msg/sec)

**증설 시점 및 기준**:
1. **Consumer Lag 모니터링**: Lag이 지속적으로 1000건 이상 누적될 경우
2. **처리 지연 시간**: 95 percentile 응답 시간이 5초 초과 시
3. **파티션 증설 공식**:
   ```
   새 파티션 수 = ⌈ 실측 TPS / Consumer당 실측 처리 TPS ⌉ + 1 (여유분)
   ```

**주의사항**:
- ⚠️ 파티션은 증설만 가능하고 축소 불가능
- ⚠️ 과도한 파티션은 Kafka 브로커 부하 증가 (메타데이터 오버헤드)
- ⚠️ Order Saga에서 파티션 증설 시 재배포 필요 (Consumer 인스턴스 수 증가)

---

### 1.3 Consumer Group 목록

#### SAGA 관련 Consumer Groups

| Consumer Group ID | 소속 서비스 | 구독 토픽 | 목적 |
|------------------|-----------|---------|------|
| `product-service-saga-forward` | Product Service | `order.created` | **Order Creation Saga: 재고 예약 처리** |
| `order-service-saga-compensation` | Order Service | `saga.stock-reservation.failed`<br>`saga.payment-initialization.failed`<br>`saga.stock-confirmation.failed` | **SAGA 보상 트랜잭션 실행** |
| `payment-service-saga-forward` | Payment Service | `order.confirmed` | **Order Creation Saga: 결제 대기 생성** |
| `order-service-saga-payment` | Order Service | `payment.completed`<br>`payment.failed` | **Payment Saga: 재고 확정 처리** |
| `payment-service-saga-compensation` | Payment Service | `saga.stock-confirmation.failed` | **Payment Saga: 결제 취소 처리** |
| `product-service-saga-compensation` | Product Service | `saga.order-confirmation.compensate`<br>`saga.payment-completion.compensate` | **재고 복원 보상 처리** |
| `saga-tracker-service` | Saga Tracker Service | `saga.tracker` | **Saga 흐름 추적 및 감사** |

#### 비즈니스 이벤트 Consumer Groups

| Consumer Group ID | 소속 서비스 | 구독 토픽 | 목적 |
|------------------|-----------|---------|------|
| `product-service-order-lifecycle` | Product Service | `order.cancelled` | 사용자 주문 취소 시 재고 복원 |
| `notification-service-payment` | Notification Service | `payment.completed`<br>`payment.failed`<br>`payment.refunded` | 결제 관련 알림 발송 |
| `notification-service-order` | Notification Service | `order.confirmed`<br>`order.expiration.notification` | 주문 관련 알림 발송 |
| `notification-service-monitoring` | Notification Service | `saga.stock-reservation.failed`<br>`stock.sync.alert` | 시스템 모니터링 알림 (Slack/PagerDuty) |
| `analytics-service` | Analytics Service | `analytics.daily.statistics` | 통계 데이터 Elasticsearch 색인 |
| `analytics-report-service` | Analytics Service | `analytics.daily.statistics` | 관리자 리포트 발송 |

---

## 2. 이벤트 스키마 명세 (Avro)

### 2.1 Schema Registry 설정

**위치**: `kafka-schemas/src/main/avro/{domain}/{EventName}.avsc`

**Schema Registry URL**: `http://schema-registry:8081`

**호환성 정책**: `BACKWARD` (이전 버전 Consumer가 새 버전 데이터 읽기 가능)

---

### 2.2 Store 도메인 이벤트

#### 2.2.1 StoreDeleted Event

**파일**: `kafka-schemas/src/main/avro/store/StoreDeleted.avsc`

```json
{
  "type": "record",
  "name": "StoreDeleted",
  "namespace": "com.groom.ecommerce.store.event.avro",
  "doc": "스토어 삭제 이벤트 - Product Service가 연관 상품 처리",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "storeId", "type": "string", "doc": "삭제된 스토어 ID"},
    {"name": "ownerId", "type": "string"},
    {"name": "deletedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Store Service
**Consumer**: Product Service (연관 상품 DISCONTINUED 상태 변경 또는 비노출 처리)
**파티션 키**: `storeId`
**용도**: 스토어 삭제 시 해당 스토어의 모든 상품 상태 변경 (Cascade Operation)

---

### 2.3 Order Creation Saga 이벤트

#### 2.3.1 OrderCreated Event

**파일**: `kafka-schemas/src/main/avro/order/OrderCreated.avsc`

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "주문 생성 이벤트 - Product Service가 재고 예약 처리",
  "fields": [
    {"name": "eventId", "type": "string", "doc": "이벤트 고유 ID (UUID) - 멱등성 보장"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string", "doc": "주문 ID (파티션 키)"},
    {"name": "userId", "type": "string"},
    {"name": "storeId", "type": "string"},
    {
      "name": "items",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "OrderItem",
          "fields": [
            {"name": "productId", "type": "string"},
            {"name": "quantity", "type": "int"},
            {
              "name": "unitPrice",
              "type": {
                "type": "bytes",
                "logicalType": "decimal",
                "precision": 10,
                "scale": 2
              }
            }
          ]
        }
      }
    },
    {
      "name": "totalAmount",
      "type": {
        "type": "bytes",
        "logicalType": "decimal",
        "precision": 10,
        "scale": 2
      }
    },
    {"name": "createdAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service
**Consumer**: Product Service
**파티션 키**: `orderId`
**Saga 흐름**: Order 생성 → Product 재고 예약

---

#### 2.3.2 StockReserved Event

**파일**: `kafka-schemas/src/main/avro/product/StockReserved.avsc`

```json
{
  "type": "record",
  "name": "StockReserved",
  "namespace": "com.groom.ecommerce.product.event.avro",
  "doc": "재고 예약 성공 이벤트 - Order Service가 주문 확정 처리",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string"},
    {
      "name": "reservedItems",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "ReservedItem",
          "fields": [
            {"name": "productId", "type": "string"},
            {"name": "quantity", "type": "int"},
            {"name": "reservedStock", "type": "int", "doc": "예약 후 남은 재고"}
          ]
        }
      }
    },
    {"name": "reservedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Product Service
**Consumer**: Order Service
**파티션 키**: `orderId`

---

#### 2.3.3 StockReservationFailed Event

**파일**: `kafka-schemas/src/main/avro/product/StockReservationFailed.avsc`

```json
{
  "type": "record",
  "name": "StockReservationFailed",
  "namespace": "com.groom.ecommerce.product.event.avro",
  "doc": "재고 예약 실패 이벤트 - Order Service가 보상 트랜잭션 실행",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string"},
    {
      "name": "failedItems",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "FailedItem",
          "fields": [
            {"name": "productId", "type": "string"},
            {"name": "requestedQuantity", "type": "int"},
            {"name": "availableStock", "type": "int"}
          ]
        }
      }
    },
    {"name": "failureReason", "type": "string", "doc": "재고 부족, 상품 미존재 등"},
    {"name": "failedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Product Service
**Consumer**: Order Service (보상 트랜잭션)
**파티션 키**: `orderId`

---

#### 2.3.4 OrderConfirmed Event

**파일**: `kafka-schemas/src/main/avro/order/OrderConfirmed.avsc`

```json
{
  "type": "record",
  "name": "OrderConfirmed",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "주문 확정 이벤트 - Payment Service가 결제 대기 생성",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string"},
    {"name": "userId", "type": "string"},
    {
      "name": "totalAmount",
      "type": {
        "type": "bytes",
        "logicalType": "decimal",
        "precision": 10,
        "scale": 2
      }
    },
    {"name": "confirmedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service
**Consumer**: Payment Service
**파티션 키**: `orderId`

---

### 2.4 Payment Saga 이벤트

#### 2.4.1 PaymentCompleted Event

**파일**: `kafka-schemas/src/main/avro/payment/PaymentCompleted.avsc`

```json
{
  "type": "record",
  "name": "PaymentCompleted",
  "namespace": "com.groom.ecommerce.payment.event.avro",
  "doc": "결제 완료 이벤트 - Order Service가 재고 확정 처리",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "paymentId", "type": "string"},
    {"name": "orderId", "type": "string", "doc": "파티션 키로 사용 (순서 보장)"},
    {"name": "userId", "type": "string"},
    {
      "name": "totalAmount",
      "type": {
        "type": "bytes",
        "logicalType": "decimal",
        "precision": 10,
        "scale": 2
      }
    },
    {
      "name": "paymentMethod",
      "type": {
        "type": "enum",
        "name": "PaymentMethod",
        "symbols": ["CARD", "BANK_TRANSFER", "KAKAO_PAY", "NAVER_PAY", "TOSS"]
      }
    },
    {"name": "pgApprovalNumber", "type": "string"},
    {"name": "completedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Payment Service (`CompletePaymentService`)
**Consumer**: Order Service, Notification Service
**파티션 키**: `orderId` (Saga 순서 보장)
**Saga 흐름**: Payment 완료 → Order 재고 확정 → Stock Confirmed/Failed

---

#### 2.4.2 PaymentFailed Event

**파일**: `kafka-schemas/src/main/avro/payment/PaymentFailed.avsc`

```json
{
  "type": "record",
  "name": "PaymentFailed",
  "namespace": "com.groom.ecommerce.payment.event.avro",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "paymentId", "type": "string"},
    {"name": "orderId", "type": "string"},
    {"name": "userId", "type": "string"},
    {"name": "failureReason", "type": "string"},
    {"name": "failedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

---

#### 2.4.3 StockConfirmed Event

**파일**: `kafka-schemas/src/main/avro/order/StockConfirmed.avsc`

```json
{
  "type": "record",
  "name": "StockConfirmed",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "재고 확정 성공 이벤트 - Payment Saga 완료",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string"},
    {"name": "paymentId", "type": "string"},
    {
      "name": "confirmedItems",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "ConfirmedOrderItem",
          "fields": [
            {"name": "productId", "type": "string"},
            {"name": "quantity", "type": "int"}
          ]
        }
      }
    },
    {"name": "confirmedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service
**Consumer**: Payment Service
**파티션 키**: `orderId`

---

#### 2.4.4 StockConfirmFailed Event

**파일**: `kafka-schemas/src/main/avro/order/StockConfirmFailed.avsc`

```json
{
  "type": "record",
  "name": "StockConfirmFailed",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "재고 확정 실패 이벤트 - Payment 보상 트랜잭션",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string"},
    {"name": "paymentId", "type": "string"},
    {"name": "failureReason", "type": "string"},
    {"name": "failedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service
**Consumer**: Payment Service (보상 트랜잭션 실행)
**파티션 키**: `orderId`

---

#### 2.4.5 PaymentCancelled Event

**파일**: `kafka-schemas/src/main/avro/payment/PaymentCancelled.avsc`

```json
{
  "type": "record",
  "name": "PaymentCancelled",
  "namespace": "com.groom.ecommerce.payment.event.avro",
  "doc": "결제 취소 이벤트 - Order Service가 주문 상태 업데이트",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "paymentId", "type": "string"},
    {"name": "orderId", "type": "string", "doc": "취소된 주문 ID"},
    {"name": "userId", "type": "string"},
    {
      "name": "cancellationReason",
      "type": {
        "type": "enum",
        "name": "PaymentCancellationReason",
        "symbols": ["STOCK_UNAVAILABLE", "ADMIN_CANCEL", "USER_CANCEL", "SYSTEM_ERROR"]
      },
      "doc": "취소 사유"
    },
    {"name": "cancelledAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Payment Service
**Consumer**: Order Service (주문 상태를 ORDER_CANCELLED로 업데이트)
**파티션 키**: `orderId`
**용도**: 재고 확정 실패로 인한 결제 보상 트랜잭션

---

### 2.5 Order Lifecycle 이벤트

#### 2.5.1 OrderCancelled Event

**파일**: `kafka-schemas/src/main/avro/order/OrderCancelled.avsc`

```json
{
  "type": "record",
  "name": "OrderCancelled",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "주문 취소 이벤트 - Product Service가 재고 복원 처리",
  "fields": [
    {"name": "eventId", "type": "string", "doc": "이벤트 고유 ID (UUID)"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string", "doc": "취소된 주문 ID"},
    {"name": "userId", "type": "string"},
    {
      "name": "items",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "CancelledOrderItem",
          "fields": [
            {"name": "productId", "type": "string"},
            {"name": "quantity", "type": "int", "doc": "복원할 재고 수량"}
          ]
        }
      }
    },
    {
      "name": "cancellationReason",
      "type": {
        "type": "enum",
        "name": "CancellationReason",
        "symbols": ["PAYMENT_TIMEOUT", "USER_REQUESTED", "STOCK_UNAVAILABLE", "SYSTEM_ERROR"]
      },
      "doc": "취소 사유"
    },
    {"name": "cancelledAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service (Scheduled Job)
**Consumer**: Product Service (재고 복원)
**파티션 키**: `orderId`
**용도**: 만료된 주문 취소 시 재고 복원 트리거

---

#### 2.5.2 OrderExpirationNotification Event

**파일**: `kafka-schemas/src/main/avro/order/OrderExpirationNotification.avsc`

```json
{
  "type": "record",
  "name": "OrderExpirationNotification",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "주문 만료 알림 이벤트 - Notification Service가 고객 알림 발송",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "orderId", "type": "string"},
    {"name": "userId", "type": "string", "doc": "알림 수신 대상"},
    {
      "name": "expirationReason",
      "type": "string",
      "default": "결제 시간 초과 (30분)",
      "doc": "만료 사유"
    },
    {"name": "expiredAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service (Scheduled Job)
**Consumer**: Notification Service
**파티션 키**: `orderId`
**용도**: 주문 만료 시 고객에게 Push 알림/이메일 발송

---

### 2.6 Monitoring 이벤트

#### 2.6.1 StockSyncAlert Event

**파일**: `kafka-schemas/src/main/avro/monitoring/StockSyncAlert.avsc`

```json
{
  "type": "record",
  "name": "StockSyncAlert",
  "namespace": "com.groom.ecommerce.monitoring.event.avro",
  "doc": "재고 불일치 알림 이벤트 - Notification Service가 Slack/PagerDuty 알림 발송",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "productId", "type": "string"},
    {"name": "dbStock", "type": "int", "doc": "DB 재고 (신뢰 데이터)"},
    {"name": "redisStock", "type": "int", "doc": "Redis 재고 (불일치 발견)"},
    {"name": "discrepancy", "type": "int", "doc": "불일치 수량 (dbStock - redisStock)"},
    {
      "name": "action",
      "type": {
        "type": "enum",
        "name": "SyncAction",
        "symbols": ["REDIS_RESTORED", "MANUAL_REVIEW_REQUIRED"]
      },
      "doc": "조치 내용"
    },
    {
      "name": "severity",
      "type": {
        "type": "enum",
        "name": "AlertSeverity",
        "symbols": ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
      },
      "default": "MEDIUM"
    },
    {"name": "detectedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Product Service (Scheduled Job)
**Consumer**: Notification Service (Slack 알림)
**파티션 키**: `productId`
**용도**: 배치 재고 동기화 작업 중 불일치 발견 시 알림

---

### 2.7 Analytics 이벤트

#### 2.7.1 DailyStatistics Event

**파일**: `kafka-schemas/src/main/avro/analytics/DailyStatistics.avsc`

```json
{
  "type": "record",
  "name": "DailyStatistics",
  "namespace": "com.groom.ecommerce.analytics.event.avro",
  "doc": "일일 통계 리포트 이벤트 - Analytics/Notification Service가 처리",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventTimestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "date", "type": "string", "doc": "통계 날짜 (YYYY-MM-DD)"},
    {"name": "totalOrders", "type": "int", "doc": "총 주문 건수"},
    {
      "name": "totalSales",
      "type": {
        "type": "bytes",
        "logicalType": "decimal",
        "precision": 15,
        "scale": 2
      },
      "doc": "총 매출액"
    },
    {
      "name": "avgOrderAmount",
      "type": {
        "type": "bytes",
        "logicalType": "decimal",
        "precision": 10,
        "scale": 2
      },
      "doc": "평균 주문 금액"
    },
    {
      "name": "topProducts",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "TopProduct",
          "fields": [
            {"name": "productId", "type": "string"},
            {"name": "productName", "type": "string"},
            {"name": "totalSold", "type": "int", "doc": "판매 수량"}
          ]
        }
      },
      "doc": "인기 상품 Top 10"
    },
    {"name": "generatedAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Producer**: Order Service (Scheduled Job)
**Consumer**: Analytics Service (Elasticsearch 색인), Notification Service (관리자 이메일)
**파티션 키**: `date`
**용도**: 일일 매출/주문 통계 집계 및 리포트 발송

---

### 2.8 SAGA 보상 트랜잭션 (Compensating Transactions)

#### 2.8.1 보상 트랜잭션 개요

**정의**: Saga 패턴에서 실패한 트랜잭션을 롤백하기 위한 보상 작업

**보상 트랜잭션 패턴**:
1. **직접 보상**: Consumer가 직접 보상 처리 (추가 이벤트 발행 생략 가능)
2. **연쇄 보상**: Consumer가 보상 이벤트를 발행하여 다른 서비스가 보상 처리

**네이밍 규칙** (v2.0):
- **실패 알림**: `saga.{트랜잭션명}.failed`
- **보상 완료**: `saga.{트랜잭션명}.compensate`
- **Saga 추적**: `saga.tracker` (단일 토픽)

#### 2.8.2 보상 트랜잭션 흐름 매트릭스 (v2.0 업데이트)

| Saga 실패 시나리오 | 트리거 이벤트 | 보상 액션 | 보상 이벤트 발행 | 후속 보상 액션 |
|------------------|--------------|----------|----------------|--------------|
| **재고 예약 실패** | `saga.stock-reservation.failed` | Order Service: 주문 취소 (ORDER_CANCELLED) | ❌ 생략* | - |
| **결제 대기 생성 실패** | `saga.payment-initialization.failed` | Order Service: 주문 취소 | ✅ `saga.order-confirmation.compensate` | Product Service: 재고 복원 |
| **결제 실패** | `payment.failed` | Order Service: 주문 취소 | ✅ `saga.order-confirmation.compensate` | Product Service: 재고 복원 |
| **재고 확정 실패** | `saga.stock-confirmation.failed` | Payment Service: 결제 취소 | ✅ `saga.payment-completion.compensate` | Order Service: 주문 취소 |

**\* 재고 예약 실패 시 보상 이벤트 발행 생략 이유**:
- 재고가 아직 예약되지 않았으므로 복원할 재고가 없음
- 주문 상태만 ORDER_CANCELLED로 변경하면 됨
- Saga Tracker 기록은 필수 (`saga.tracker`)

#### 2.8.3 보상 트랜잭션 상세 플로우 (v2.0 업데이트)

##### Flow 1: 결제 실패 → 주문 취소 → 재고 복원

```
1. Payment Service → Kafka: payment.failed (Forward 실패 이벤트)
   {paymentId, orderId, failureReason: "잔액 부족"}

2. Order Service (Consumer):
   - 주문 상태: ORDER_CONFIRMED → ORDER_CANCELLED
   - 보상 이벤트 발행 → saga.order-confirmation.compensate

3. Product Service (Consumer):
   - 재고 복원: reserved_stock -= cancelled_quantity
   - Saga Tracker 기록 → saga.tracker
```

**이벤트 체인**:
```
payment.failed → saga.order-confirmation.compensate → (재고 복원 완료)
```

---

##### Flow 2: 재고 확정 실패 → 결제 취소 → 주문 취소

```
1. Order Service → Kafka: saga.stock-confirmation.failed
   {orderId, paymentId, reason: "재고 부족"}

2. Payment Service (Consumer):
   - 결제 상태: PAYMENT_COMPLETED → PAYMENT_CANCELLED
   - PG사 취소 API 호출
   - 보상 이벤트 발행 → saga.payment-completion.compensate

3. Order Service (Consumer):
   - 주문 상태: ORDER_CONFIRMED → ORDER_CANCELLED
   - 보상 이벤트 발행 → saga.order-confirmation.compensate

4. Product Service (Consumer):
   - 재고 복원: reserved_stock -= cancelled_quantity
   - Saga Tracker 기록 → saga.tracker
```

**이벤트 체인**:
```
saga.stock-confirmation.failed
  → saga.payment-completion.compensate
    → saga.order-confirmation.compensate
      → (재고 복원 완료)
```

---

##### Flow 3: 재고 예약 실패 → 주문 취소 (직접 보상)

```
1. Product Service → Kafka: saga.stock-reservation.failed
   {orderId, failedItems, reason: "재고 부족"}

2. Order Service (Consumer):
   - 주문 상태: ORDER_CREATED → ORDER_CANCELLED
   - 보상 이벤트 발행 ❌ 생략 (재고 복원 불필요)
   - Saga Tracker 기록 → saga.tracker
   - 고객 알림 발송
```

**이벤트 체인**:
```
saga.stock-reservation.failed → (주문 취소 완료, 보상 이벤트 생략)
```

---

##### Flow 4: 결제 대기 생성 실패 → 주문 취소 → 재고 복원

```
1. Payment Service → Kafka: saga.payment-initialization.failed
   {orderId, failureReason: "DB 장애"}

2. Order Service (Consumer):
   - 주문 상태: ORDER_CONFIRMED → ORDER_CANCELLED
   - 보상 이벤트 발행 → saga.order-confirmation.compensate

3. Product Service (Consumer):
   - 재고 복원: reserved_stock -= cancelled_quantity
   - Saga Tracker 기록 → saga.tracker
```

**이벤트 체인**:
```
saga.payment-initialization.failed
  → saga.order-confirmation.compensate
    → (재고 복원 완료)
```

---

#### 2.8.4 보상 트랜잭션 구현 가이드 (v2.0)

**보상 이벤트 발행이 필요한 경우**:
1. ✅ 다른 서비스가 보상 작업을 수행해야 하는 경우
   - 예: 결제 취소 후 주문도 취소해야 함 → `saga.payment-completion.compensate` 발행
   - 예: 주문 취소 후 재고 복원이 필요함 → `saga.order-confirmation.compensate` 발행

2. ✅ 보상 작업을 다른 서비스에게 알려야 하는 경우
   - 예: 결제 취소 사실을 Order Service가 알아야 함

**보상 이벤트 발행이 생략 가능한 경우**:
1. ✅ 해당 서비스에서 보상 작업이 완료되는 경우
   - 예: 재고 예약 전 실패 → 복원할 재고가 없음
   - 예: 최상위 단계 (더 이상 보상할 상위 단계 없음)
   - **단, Saga Tracker 기록은 필수**

2. ❌ 순환 이벤트 발행 위험이 있는 경우
   - 예: A → B → A 순환 참조 방지

**Saga Tracker 기록 (필수)**:
- 모든 보상 단계는 `saga.tracker` 토픽에 기록
- Payload: `{sagaId, sagaType, step, status: "COMPENSATED", timestamp}`

#### 2.8.5 보상 트랜잭션 테스트 시나리오

**필수 테스트 케이스**:
1. ✅ 재고 부족 시 주문 자동 취소 확인
2. ✅ 결제 실패 시 주문 취소 + 재고 복원 확인
3. ✅ 재고 확정 실패 시 결제 취소 + 주문 취소 확인
4. ✅ 보상 이벤트 멱등성 확인 (중복 처리 방지)
5. ✅ 보상 트랜잭션 실패 시 DLT 전송 확인

---

## 3. Spring Cloud Contract 명세

### 3.1 Contract 개요

**Spring Cloud Contract**는 Producer와 Consumer 간 계약을 정의하고 자동으로 테스트를 생성합니다.

**구조**:
```
kafka-schemas/
└── src/
    ├── main/avro/                    # Avro 스키마
    └── test/resources/contracts/     # Spring Cloud Contract
        ├── product/
        │   ├── shouldPublishProductCreated.groovy
        │   └── shouldConsumeProductCreated.groovy
        ├── payment/
        └── user/
```

---

### 3.2 Producer Contract 예시

#### ProductCreated Event Producer Contract

**파일**: `kafka-schemas/src/test/resources/contracts/product/shouldPublishProductCreated.groovy`

```groovy
package contracts.product

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Product Service가 ProductCreated 이벤트를 발행한다"

    label "product.created"

    input {
        // Producer 트리거: Product 생성 API 호출
        triggeredBy("registerProduct()")
    }

    outputMessage {
        sentTo "product.created"

        // Partition Key
        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))))
        }

        // Avro 스키마 기반 Body
        body([
            eventId: $(producer(anyUuid()), consumer("550e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            productId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            storeId: $(producer(anyUuid()), consumer("770e8400-e29b-41d4-a716-446655440000")),
            storeName: $(producer(anyNonEmptyString()), consumer("My Store")),
            name: $(producer(anyNonEmptyString()), consumer("Sample Product")),
            price: $(producer(anyDouble()), consumer(10000.00)),
            stockQuantity: $(producer(anyPositiveInt()), consumer(50)),
            status: $(producer(anyOf("SELLING", "SOLD_OUT", "DISCONTINUED")), consumer("SELLING")),
            category: $(producer(optional(anyNonEmptyString())), consumer("Electronics")),
            createdAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}
```

**Producer 테스트 자동 생성**:
- Contract 기반으로 `ProductCreatedTest` 자동 생성
- `registerProduct()` 메서드 호출 시 이벤트 발행 검증
- Stub을 Consumer에게 제공

---

### 3.3 Consumer Contract 예시

#### ProductCreated Event Consumer Contract

**파일**: `kafka-schemas/src/test/resources/contracts/product/shouldConsumeProductCreated.groovy`

```groovy
package contracts.product

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Analytics Service가 ProductCreated 이벤트를 소비한다"

    label "analytics.service.product.created"

    input {
        // Consumer 입력: Kafka 메시지
        messageFrom "product.created"

        messageHeaders {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")
        }

        messageBody([
            eventId: "550e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            productId: "660e8400-e29b-41d4-a716-446655440000",
            storeId: "770e8400-e29b-41d4-a716-446655440000",
            storeName: "My Store",
            name: "Sample Product",
            price: 10000.00,
            stockQuantity: 50,
            status: "SELLING",
            category: "Electronics",
            createdAt: 1705060800000L
        ])
    }

    outputMessage {
        // Consumer 검증: 감사 로그가 저장되었는지 확인
        assertThat("auditLogRepository.existsByEventId(UUID.fromString('550e8400-e29b-41d4-a716-446655440000'))")
    }
}
```

**Consumer 테스트 자동 생성**:
- Contract 기반으로 `ProductCreatedConsumerTest` 자동 생성
- Stub 메시지로 Consumer 동작 검증
- DB에 감사 로그 저장 검증

---

### 3.4 Saga Contract 예시

#### PaymentCompleted → StockConfirmed Saga

**Producer Contract** (`payment/shouldPublishPaymentCompleted.groovy`):

```groovy
Contract.make {
    description "Payment Service가 결제 완료 이벤트를 발행한다"
    label "payment.completed"

    input {
        triggeredBy("completePayment()")
    }

    outputMessage {
        sentTo "payment.completed"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // orderId
        }

        body([
            eventId: $(producer(anyUuid())),
            eventTimestamp: $(producer(anyPositiveInt())),
            paymentId: $(producer(anyUuid())),
            orderId: $(producer(anyUuid()), consumer("880e8400-e29b-41d4-a716-446655440000")),
            userId: $(producer(anyUuid())),
            totalAmount: $(producer(anyDouble())),
            paymentMethod: "CARD",
            pgApprovalNumber: "APPROVAL-12345",
            completedAt: $(producer(anyPositiveInt()))
        ])
    }
}
```

**Consumer Contract** (`order/shouldConsumePaymentCompleted.groovy`):

```groovy
Contract.make {
    description "Order Service가 결제 완료 이벤트를 소비하고 재고를 확정한다"
    label "order.service.payment.completed"

    input {
        messageFrom "payment.completed"

        messageBody([
            eventId: "990e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            paymentId: "aa0e8400-e29b-41d4-a716-446655440000",
            orderId: "880e8400-e29b-41d4-a716-446655440000",
            userId: "bb0e8400-e29b-41d4-a716-446655440000",
            totalAmount: 50000.00,
            paymentMethod: "CARD",
            pgApprovalNumber: "APPROVAL-12345",
            completedAt: 1705060800000L
        ])
    }

    outputMessage {
        sentTo "order.stock.confirmed"

        body([
            eventId: $(anyUuid()),
            eventTimestamp: $(anyPositiveInt()),
            orderId: "880e8400-e29b-41d4-a716-446655440000",
            paymentId: "aa0e8400-e29b-41d4-a716-446655440000",
            confirmedItems: [
                [productId: $(anyUuid()), quantity: $(anyPositiveInt())]
            ],
            confirmedAt: $(anyPositiveInt())
        ])
    }
}
```

---

### 3.5 Contract 테스트 실행

**Producer 테스트** (Product Service):
```bash
# Product Service
./gradlew :product-service:generateContractTests
./gradlew :product-service:test

# Stub 생성 및 로컬 Maven 저장
./gradlew :product-service:publishStubsToScm
```

**Consumer 테스트** (Order Service):
```bash
# Order Service
# Producer Stub 다운로드
./gradlew :order-service:copyContracts

# Contract 기반 Consumer 테스트 실행
./gradlew :order-service:test
```

---

## 4. 동기 HTTP API 명세

### 4.1 User 조회 API

**Producer**: User Service
**Consumer**: Store Service

**엔드포인트**: `GET /api/v1/users/{userId}`

**Request**:
```http
GET /api/v1/users/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Host: user-service:8080
Authorization: Bearer {service-token}
```

**Response**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "OWNER",
  "status": "ACTIVE",
  "createdAt": "2025-01-10T12:00:00Z"
}
```

**Spring Cloud Contract** (`user/shouldGetUser.groovy`):
```groovy
Contract.make {
    request {
        method GET()
        url("/api/v1/users/550e8400-e29b-41d4-a716-446655440000")
        headers {
            accept(applicationJson())
            header("Authorization", "Bearer test-token")
        }
    }

    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            userId: "550e8400-e29b-41d4-a716-446655440000",
            username: "johndoe",
            email: "john@example.com",
            role: "OWNER",
            status: "ACTIVE",
            createdAt: "2025-01-10T12:00:00Z"
        ])
    }
}
```

---

### 4.2 Product 조회 API

**Producer**: Product Service
**Consumer**: Order Service

**엔드포인트**: `GET /api/v1/products/{productId}`

**Response**:
```json
{
  "productId": "660e8400-e29b-41d4-a716-446655440000",
  "storeId": "770e8400-e29b-41d4-a716-446655440000",
  "storeName": "My Store",
  "name": "Product Name",
  "price": 10000.00,
  "stockQuantity": 50,
  "status": "SELLING",
  "category": "Electronics"
}
```

---

### 4.3 Store 조회 API

**Producer**: Store Service
**Consumer**: Order Service

**엔드포인트**: `GET /api/v1/stores/{storeId}`

**Response**:
```json
{
  "storeId": "770e8400-e29b-41d4-a716-446655440000",
  "ownerId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Store",
  "status": "LAUNCHED",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

---

## 5. 구현 가이드

### 5.1 Kafka Producer 구현

**의존성** (`build.gradle.kts`):
```kotlin
dependencies {
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-avro-serializer:7.5.0")
    implementation("org.apache.avro:avro:1.11.3")
}
```

**설정** (`application.yml`):
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://localhost:8081
```

**Producer 코드**:
```kotlin
@Service
class RegisterProductService(
    private val productRepository: ProductRepositoryImpl,
    private val kafkaTemplate: KafkaTemplate<String, ProductCreated>
) {
    @Transactional
    fun register(command: RegisterProductCommand): RegisterProductResult {
        // 1. Product 생성
        val product = Product(...).let(productRepository::save)

        // 2. Avro 이벤트 생성
        val event = ProductCreated.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventTimestamp(System.currentTimeMillis())
            .setProductId(product.id.toString())
            .setStoreId(product.storeId.toString())
            .setStoreName(product.storeName)
            .setName(product.name)
            .setPrice(ByteBuffer.wrap(product.price.unscaledValue().toByteArray()))
            .setStockQuantity(product.stockQuantity)
            .setStatus(ProductStatus.valueOf(product.status.name))
            .setCategory(product.category)
            .setCreatedAt(product.createdAt!!.toInstant().toEpochMilli())
            .build()

        // 3. Kafka 발행 (파티션 키: productId)
        kafkaTemplate.send("product.created", product.id.toString(), event)

        return RegisterProductResult(...)
    }
}
```

---

### 5.2 Kafka Consumer 구현

**설정** (`application.yml`):
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: analytics-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://localhost:8081
        specific.avro.reader: true
```

**Consumer 코드**:
```kotlin
@Component
class ProductCreatedEventHandler(
    private val auditLogRepository: AuditLogRepository
) {
    @KafkaListener(
        topics = ["product.created"],
        groupId = "analytics-service"
    )
    @Transactional
    fun handle(@Payload event: ProductCreated) {
        // 멱등성 보장
        val eventId = UUID.fromString(event.eventId)
        if (auditLogRepository.existsByEventId(eventId)) {
            logger.warn("Event already processed: $eventId")
            return
        }

        // 감사 로그 저장
        val auditLog = AuditLog(
            eventId = eventId,
            eventType = "PRODUCT_CREATED",
            aggregateId = UUID.fromString(event.productId),
            aggregateType = "PRODUCT",
            payload = objectMapper.writeValueAsString(event),
            occurredAt = Instant.ofEpochMilli(event.eventTimestamp)
        )
        auditLogRepository.save(auditLog)

        logger.info("Product created event logged: ${event.productId}")
    }
}
```

---

### 5.3 Spring Cloud Contract 설정

**Producer 설정** (`build.gradle.kts`):
```kotlin
plugins {
    id("org.springframework.cloud.contract") version "4.1.0"
}

contracts {
    testFramework.set(org.springframework.cloud.contract.verifier.config.TestFramework.JUNIT5)
    packageWithBaseClasses.set("com.groom.ecommerce.product.contracts")
    baseClassForTests.set("com.groom.ecommerce.product.contracts.BaseContractTest")
}

dependencies {
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
}
```

**Base Contract Test**:
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseContractTest {
    @Autowired
    lateinit var registerProductService: RegisterProductService

    fun registerProduct() {
        registerProductService.register(RegisterProductCommand(...))
    }
}
```

**Consumer 설정** (`build.gradle.kts`):
```kotlin
dependencies {
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}
```

**Consumer Test**:
```kotlin
@SpringBootTest
@AutoConfigureStubRunner(
    ids = ["com.groom.ecommerce:product-service:+:stubs:8080"],
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class ProductCreatedConsumerTest {
    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, ProductCreated>

    @Test
    fun `should consume ProductCreated event`() {
        // Stub 메시지 발행
        val event = ProductCreated.newBuilder()...build()
        kafkaTemplate.send("product.created", "test-id", event)

        // 검증: 감사 로그 생성 확인
        await().atMost(5, TimeUnit.SECONDS).until {
            auditLogRepository.existsByEventId(UUID.fromString("event-id"))
        }
    }
}
```

---

## 6. 체크리스트

### Kafka 담당자

- [ ] 비즈니스 이벤트 토픽 생성 (Section 1.1)
- [ ] **SAGA 이벤트 토픽 생성** (Section 1.1)
  - [ ] `saga.*.failed` 토픽
  - [ ] `saga.*.compensate` 토픽
  - [ ] `saga.tracker` 토픽
- [ ] Dead Letter Topics 생성
- [ ] Schema Registry 설정 및 확인
- [ ] Consumer Group 모니터링 설정 (SAGA 전용 Consumer Group 포함)
- [ ] Retention 정책 확인
  - [ ] 비즈니스 이벤트: 7일
  - [ ] SAGA 이벤트: 7일
  - [ ] Saga Tracker: 30일 (감사 목적)

### Spring Cloud Contract 담당자

- [ ] Contract 정의 작성 (Section 3)
- [ ] Producer Contract 테스트 실행
- [ ] Consumer Contract 테스트 실행
- [ ] **SAGA 보상 이벤트 Contract 작성**
- [ ] Stub 생성 및 공유 (Maven/Nexus)
- [ ] CI/CD 파이프라인 통합

### 백엔드 개발자

- [ ] Avro 스키마 구현 (Section 2)
- [ ] Producer 이벤트 발행 (Section 5.1)
- [ ] Consumer 이벤트 소비 (Section 5.2)
- [ ] **SAGA 보상 트랜잭션 구현** (Section 2.8)
  - [ ] `.failed` 이벤트 Consumer 구현
  - [ ] `.compensate` 이벤트 Producer 구현
  - [ ] Saga Tracker 기록 로직 추가
- [ ] 멱등성 보장 (`eventId` 기반 중복 처리 방지)
- [ ] 에러 처리 및 Retry 로직
- [ ] 보상 트랜잭션 테스트 (Section 2.8.5)

---

## 7. v2.0 주요 변경사항 요약

### 네이밍 규칙 변경

| 구분 | v1.0 (구) | v2.0 (신) | 변경 사유 |
|------|----------|----------|---------|
| 재고 예약 실패 | `stock.reservation.failed` | `saga.stock-reservation.failed` | SAGA 이벤트 명확히 구분 |
| 재고 확정 실패 | `order.stock.confirm.failed` | `saga.stock-confirmation.failed` | SAGA 이벤트 명확히 구분 |
| 결제 대기 실패 | (없음) | `saga.payment-initialization.failed` | 명시적 실패 이벤트 추가 |
| 결제 취소 (보상) | `payment.cancelled` | `saga.payment-completion.compensate` | 사용자 환불과 구분 |
| 주문 취소 (보상) | (혼재) | `saga.order-confirmation.compensate` | 사용자 취소와 구분 |
| 재고 복원 (보상) | (없음) | `saga.stock-reservation.compensate` | 명시적 보상 이벤트 |
| Saga Tracker | (없음) | `saga.tracker` | Saga 흐름 추적 및 감사 |

### 추가된 Consumer Groups

- `order-service-saga-compensation`: SAGA 보상 처리 전담
- `payment-service-saga-compensation`: Payment 보상 처리 전담
- `product-service-saga-compensation`: 재고 복원 보상 처리 전담
- `saga-tracker-service`: Saga 흐름 추적 및 감사

### 비즈니스 이벤트 명확화

- `order.cancelled`: 사용자/관리자 주문 취소 (비즈니스 로직)
- `payment.refunded`: 사용자 환불 요청 (새로 추가)

---

**참고 문서**:
- `카프카 네이밍 규칙(with.SAGA패턴).md` - 네이밍 규칙 상세
- `카프카+SAGA패턴 토픽 네이밍 전략.md` - 설계 배경 및 의사결정 과정

**문의**: #kafka-events 채널
**문서 업데이트**: 이 문서를 수정 후 PR 제출
**마지막 업데이트**: 2025-11-11 (v2.0)
