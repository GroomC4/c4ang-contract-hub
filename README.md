# C4ang Contract Hub

> MSA 서비스 간 통신을 위한 Avro 스키마 및 문서 중앙 관리 레포지토리

## 📋 프로젝트 개요

C4ang Contract Hub는 **MSA(Microservices Architecture)** 환경에서 서비스 간 통신을 위한 **중앙 집중식 스키마 및 문서 관리 시스템**입니다.

**관리 대상:**
- **비동기 통신**: Kafka 이벤트 기반 통신 (SAGA 패턴)
- **동기 통신**: K8s 내부 REST API 통신

Apache Avro 스키마로 데이터 구조를 정의하고, Java class를 자동 생성하여 각 서비스에서 타입 안전하게 사용할 수 있도록 지원합니다.

## 🛠️ 기술 스택

- **Schema Definition**: Apache Avro (.avsc)
- **Code Generation**: Avro → Java Classes (Kotlin에서 사용 가능)
- **Serialization**: Apache Avro + Confluent Schema Registry
- **Build Tool**: Gradle 8.5 (Kotlin DSL)
- **Language**: Java 21
- **Distribution**: JitPack / Maven Local

## 🎯 핵심 책임

### 1. Avro 스키마 관리

**서비스 간 통신을 위한 Avro 스키마를 중앙에서 관리합니다.**

**관리 대상:**
- **비동기 이벤트**: Kafka 이벤트 스키마 (SAGA 패턴, 보상 트랜잭션)
- **동기 API**: REST API 요청/응답 스키마

**주요 기능:**
- 도메인별 Avro 스키마 정의 (`.avsc`)
- 이벤트/API 스키마 분리 관리
- 스키마 버전 관리 (Git)
- 공통 스키마 재사용 (각 영역별 독립 관리)

**장점:**
- 단일 진실 공급원(Single Source of Truth)
- 스키마 진화(Schema Evolution) 추적
- 타입 안전성 보장
- Breaking Change 사전 감지

### 2. Java Class 생성 및 배포

**Avro 스키마로부터 Java class를 자동 생성하고 배포합니다.**

**주요 기능:**
- Gradle Avro Plugin을 통한 Java class 생성 (SpecificRecord)
- JitPack을 통한 artifact 배포
- Producer/Consumer 서비스에서 의존성으로 추가 가능
- Kotlin 프로젝트에서도 사용 가능
- Confluent Kafka Avro Serializer 지원

**장점:**
- 수동 DTO 작성 불필요
- Producer-Consumer 간 타입 일치 보장
- IDE 자동완성 및 타입 체크
- 직렬화/역직렬화 자동 처리
- Kotlin interop 완벽 지원

### 3. 통신 흐름 문서화

**서비스 간 통신 흐름을 시각화하고 문서화합니다.**

**비동기 이벤트 흐름 (Event Flows):**
- 비즈니스 플로우별 이벤트 시퀀스 문서화
- Kafka 토픽 및 Partition Key 명세
- SAGA 패턴 및 보상 트랜잭션 정의
- 이벤트 발행/구독 관계 다이어그램

**동기 API 흐름 (API Flows):**
- K8s 내부 REST API 명세
- 요청/응답 스키마 및 에러 처리
- 서비스 간 호출 시퀀스 다이어그램
- 성능 및 보안 고려사항

**장점:**
- 분산 시스템 흐름의 가시성 확보
- 아키텍처 복잡도 관리
- 신규 개발자의 시스템 이해도 향상
- 장애 발생 시 디버깅 용이

## 🏗️ 프로젝트 구조

```
c4ang-contract-hub/
│
├── src/main/
│   ├── events/avro/            # 비동기 이벤트 스키마 (Kafka)
│   │   ├── order/              # 주문 도메인 이벤트
│   │   │   ├── OrderCreated.avsc
│   │   │   ├── OrderConfirmed.avsc
│   │   │   └── OrderCancelled.avsc
│   │   ├── payment/            # 결제 도메인 이벤트
│   │   │   ├── PaymentCompleted.avsc
│   │   │   ├── PaymentFailed.avsc
│   │   │   └── PaymentCancelled.avsc
│   │   ├── product/            # 상품 도메인 이벤트
│   │   │   └── StockReserved.avsc
│   │   ├── store/              # 매장 도메인 이벤트
│   │   │   └── StoreDeleted.avsc
│   │   ├── saga/               # SAGA 패턴 보상 트랜잭션
│   │   │   ├── SagaTracker.avsc
│   │   │   └── StockReservationFailed.avsc
│   │   ├── monitoring/         # 모니터링 이벤트
│   │   │   └── StockSyncAlert.avsc
│   │   ├── analytics/          # 분석 이벤트
│   │   │   └── DailyStatistics.avsc
│   │   └── common/             # 이벤트 공통 스키마
│   │       └── EventMetadata.avsc
│   │
│   └── api/avro/               # 동기 API 스키마 (HTTP REST)
│       ├── customer/           # Customer Service API
│       │   ├── UserInternalResponse.avsc
│       │   └── UserProfileInternal.avsc
│       ├── order/              # Order Service API (추후 추가)
│       ├── store/              # Store Service API (추후 추가)
│       └── common/             # API 공통 스키마
│           ├── ErrorResponse.avsc
│           └── Pagination.avsc
│
├── event-flows/                # 비동기 이벤트 흐름 문서
│   ├── order-creation/         # 주문 생성 SAGA
│   ├── payment-processing/     # 결제 처리 SAGA
│   ├── store-management/       # 매장 관리
│   └── scheduled-jobs/         # 스케줄 작업
│
├── api-flows/                  # 동기 API 흐름 문서
│   ├── customer-service/       # Customer Service API
│   │   ├── README.md
│   │   └── internal-user-api.md
│   ├── order-service/          # Order Service API (추후 추가)
│   └── store-service/          # Store Service API (추후 추가)
│
├── docs/                       # 상세 가이드
│   ├── interface/              # 인터페이스 명세
│   │   ├── kafka-event-specifications.md  # 이벤트 명세 (v2.0)
│   │   └── kafka-event-sequence.md        # 이벤트 시퀀스 다이어그램
│   └── publishing/             # 배포 가이드
│       ├── jitpack-publishing-guide.md    # JitPack 배포
│       └── avro-artifact-publishing.md    # Avro 클래스 배포
│
├── buildSrc/                   # 빌드 스크립트
│   └── src/main/kotlin/
│       └── AvroDocGenerator.kt # Avro 문서 자동 생성
│
└── build/
    └── generated-main-avro-java/  # Avro 생성 Java 클래스
        └── com/groom/ecommerce/
            ├── order/event/avro/
            │   └── OrderCreated.java
            ├── payment/event/avro/
            │   └── PaymentCompleted.java
            ├── saga/event/avro/
            │   └── SagaTracker.java
            └── ...
```

## 🚀 시작하기

### 사전 요구사항

- JDK 21 이상
- Gradle 8.5 이상

### 빌드 및 배포

```bash
# 1. Avro 스키마로부터 Java 클래스 생성
./gradlew generateAvroJava

# 2. 빌드
./gradlew build

# 3. 로컬 Maven 저장소에 배포 (로컬 개발용)
./gradlew publishToMavenLocal
```

### JitPack 배포

[![](https://jitpack.io/v/GroomC4/c4ang-contract-hub.svg)](https://jitpack.io/#GroomC4/c4ang-contract-hub)

현재 최신 버전: **v1.0.0**

```bash
# 1. Git Tag 생성 및 Push
git tag v1.0.0
git push origin v1.0.0

# 2. JitPack 자동 빌드
# https://jitpack.io/#GroomC4/c4ang-contract-hub
```

**상세 가이드**: [JitPack 배포 가이드](docs/publishing/jitpack-publishing-guide.md)

## 📖 사용 가이드

### Producer/Consumer 서비스에서 사용하기

#### 1. 의존성 추가

```kotlin
// build.gradle.kts

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }  // JitPack 저장소 추가
}

dependencies {
    // Contract Hub: 이벤트 + API 스키마 (v1.0.0)
    implementation("com.github.GroomC4:c4ang-contract-hub:v1.0.0")

    // Kafka 및 Avro 의존성 (이벤트용)
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
}
```

#### 2. Producer에서 이벤트 발행

```kotlin
// Order Service (Producer)
import com.groom.ecommerce.order.event.avro.OrderCreated
import org.springframework.kafka.core.KafkaTemplate

@Service
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, OrderCreated>
) {
    fun publishOrderCreated(order: Order) {
        val event = OrderCreated.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventTimestamp(System.currentTimeMillis())
            .setOrderId(order.id)
            .setCustomerId(order.customerId)
            .setItems(order.items.map { /* ... */ })
            .setCreatedAt(order.createdAt.toEpochMilli())
            .build()

        kafkaTemplate.send("order.created", order.id, event)
    }
}
```

#### 3. 비동기 이벤트 구독 (Kafka)

```kotlin
// Payment Service (Event Consumer)
import com.groom.ecommerce.order.event.avro.OrderCreated
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class OrderEventListener {

    @KafkaListener(topics = ["order.created"], groupId = "payment-service")
    fun handleOrderCreated(event: OrderCreated) {
        val orderId = event.orderId
        val userId = event.userId

        // 결제 처리 로직
        processPayment(orderId, userId)
    }
}
```

#### 4. 동기 API 호출 (HTTP)

```kotlin
// Order Service (API Client)
import com.groom.ecommerce.customer.api.avro.UserInternalResponse
import com.groom.ecommerce.customer.api.avro.UserRole
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class CustomerServiceClient(
    private val restTemplate: RestTemplate
) {
    fun getUserById(userId: String): UserInternalResponse? {
        return restTemplate.getForObject(
            "http://customer-service/internal/v1/users/$userId",
            UserInternalResponse::class.java
        )
    }

    fun validateOwner(userId: String): Boolean {
        val user = getUserById(userId) ?: return false
        return user.role == UserRole.OWNER && user.isActive
    }
}
```

### Avro 스키마 개발

#### 1. 새로운 스키마 추가

```bash
# 비동기 이벤트 스키마 추가
vi src/main/events/avro/order/OrderCreated.avsc

# 동기 API 스키마 추가
vi src/main/api/avro/customer/UserInternalResponse.avsc

# Java 클래스 생성
./gradlew generateAvroJava

# 생성된 클래스 확인
# build/generated-main-avro-java/com/groom/ecommerce/{domain}/event/avro/
# build/generated-main-avro-java/com/groom/ecommerce/{domain}/api/avro/
```

#### 2. 스키마 예시

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.groom.ecommerce.order.event.avro",
  "doc": "주문 생성 이벤트 - Order Creation Saga의 시작점",
  "fields": [
    {
      "name": "eventId",
      "type": "string",
      "doc": "이벤트 고유 ID (UUID) - 멱등성 보장"
    },
    {
      "name": "eventTimestamp",
      "type": "long",
      "logicalType": "timestamp-millis",
      "doc": "이벤트 발생 시각 (epoch millis)"
    },
    {
      "name": "orderId",
      "type": "string",
      "doc": "주문 ID (Partition Key)"
    }
  ]
}
```

**상세 가이드**: [Avro 클래스 배포 가이드](docs/publishing/avro-artifact-publishing.md)

### 통신 흐름 문서

서비스 간 통신의 전체 흐름은 다음 문서를 참고하세요:

**비동기 이벤트:**
- **[Event Flows](event-flows/README.md)** - 이벤트 흐름 전체 개요
- **[Kafka 이벤트 명세 v2.0](docs/interface/kafka-event-specifications.md)** - 전체 이벤트 목록 및 상세 명세
- **[Kafka 이벤트 시퀀스](docs/interface/kafka-event-sequence.md)** - 기능별 이벤트 흐름 다이어그램

**동기 API:**
- **[API Flows](api-flows/README.md)** - API 흐름 전체 개요
- **[Customer Service API](api-flows/customer-service/)** - 사용자 조회 API

**주요 SAGA 흐름:**
1. **주문 생성 Saga**: Order Created → Stock Reserved → Order Confirmed
2. **결제 완료 Saga**: Payment Completed → Stock Confirmed
3. **보상 트랜잭션**: 실패 시 역순 보상 이벤트 발행

## 🔄 Spring Cloud Contract Test

**Spring Cloud Contract Test는 각 서비스에서 수행합니다.**

### 각 서비스에서 Contract Test 작성

```kotlin
// order-service/src/test/resources/contracts/produce_order_created.kts

import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 생성 시 OrderCreated 이벤트 발행"

    input {
        triggeredBy("createOrder()")
    }

    outputMessage {
        sentTo("order.created")
        body("""
            {
                "eventId": "${value(consumer(regex("[0-9a-f-]{36}")), producer("123e4567-e89b-12d3-a456-426614174000"))}",
                "orderId": "ORD-123",
                "customerId": "CUST-001"
            }
        """.trimIndent())
        headers {
            header("kafka_messageKey", "ORD-123")
        }
    }
}
```

### Contract Test 실행

```bash
# 각 서비스에서
./gradlew contractTest
```

**이점:**
- 각 서비스가 자신의 contract만 관리
- c4ang-contract-hub는 스키마만 제공
- 서비스별 독립적인 테스트 실행

## 🔄 CI/CD 및 버전 관리

### GitHub Actions 자동화

이 프로젝트는 GitHub Actions를 통해 자동 빌드 및 배포를 수행합니다.

**Workflow:**
1. **PR 검증** (`pr-validation.yml`)
   - Avro 스키마 유효성 검증
   - 빌드 테스트
   - 생성된 클래스 확인

2. **브랜치 빌드** (`branch-build.yml`)
   - main/develop 브랜치 push 시 자동 빌드
   - Avro 클래스 생성 확인

3. **릴리스 배포** (`release.yml`)
   - Git Tag 생성 시 자동 실행
   - JitPack 배포

### 버전 관리 전략

- **Semantic Versioning**: `v{major}.{minor}.{patch}`
- **Git Tag 기반 배포**: `v1.0.0`, `v1.1.0`, etc.
- **Breaking Change**: Major 버전 증가 시 명시

## 📚 관련 문서

### 통신 흐름
- [Event Flows](event-flows/README.md) - 비동기 이벤트 흐름
- [API Flows](api-flows/README.md) - 동기 API 흐름

### 인터페이스 명세
- [Kafka 이벤트 명세 v2.0](docs/interface/kafka-event-specifications.md)
- [Kafka 이벤트 시퀀스](docs/interface/kafka-event-sequence.md)

### 배포 가이드
- [JitPack 배포 가이드](docs/publishing/jitpack-publishing-guide.md)
- [Avro Artifact 배포 가이드](docs/publishing/avro-artifact-publishing.md)

## 🤝 기여하기

### 새로운 이벤트 추가 프로세스

1. **스키마 작성**
   - `src/main/events/avro/{domain}/` 에 `.avsc` 파일 추가
   - 네이밍: `{EventName}.avsc` (PascalCase)
   - namespace: `com.groom.ecommerce.{domain}.event.avro`

2. **문서 업데이트**
   - `event-flows/` 에 이벤트 흐름 문서 추가
   - `docs/interface/kafka-event-specifications.md` 에 이벤트 명세 추가

3. **빌드 및 테스트**
   ```bash
   ./gradlew generateAvroJava
   ./gradlew build
   ```

### 새로운 API 추가 프로세스

1. **스키마 작성**
   - `src/main/api/avro/{service}/` 에 `.avsc` 파일 추가
   - 네이밍: `{ResponseName}.avsc` (PascalCase)
   - namespace: `com.groom.ecommerce.{service}.api.avro`

2. **문서 업데이트**
   - `api-flows/{service-name}/` 에 API 문서 추가
   - 시퀀스 다이어그램 및 사용 예시 작성

3. **빌드 및 테스트**
   ```bash
   ./gradlew generateAvroJava
   ./gradlew build
   ```

4. **PR 생성**
   - 변경 사항 설명
   - Breaking Change 여부 명시
   - 영향받는 서비스 목록

## 📝 License

This project is licensed under the MIT License.

## 📧 Contact

프로젝트 관련 문의: [GitHub Issues](https://github.com/GroomC4/c4ang-contract-hub/issues)
