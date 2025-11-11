# Avro 클래스 배포 및 사용 가이드

## 개요

이 프로젝트는 두 가지 Artifact를 배포합니다:

1. **Contract Stubs** (`c4ang-contract-stubs`) - Spring Cloud Contract Stub
2. **Avro 클래스** (`c4ang-avro-events`) - Kafka 이벤트 데이터 클래스 (신규 추가)

다른 서비스(Producer, Consumer)는 이 Avro 클래스를 의존성으로 추가하여 사용할 수 있습니다.

---

## 배포 방법

### 1. Maven Local에 배포 (로컬 테스트)

```bash
# Contract Hub 프로젝트에서
./gradlew publishToMavenLocal
```

**결과**:
```
~/.m2/repository/com/c4ang/
├── c4ang-contract-stubs/
│   └── 1.0.0-SNAPSHOT/
│       └── c4ang-contract-stubs-1.0.0-SNAPSHOT-stubs.jar
└── c4ang-avro-events/
    └── 1.0.0-SNAPSHOT/
        ├── c4ang-avro-events-1.0.0-SNAPSHOT.jar       ← Avro 클래스
        └── c4ang-avro-events-1.0.0-SNAPSHOT-sources.jar
```

### 2. 회사 내부 Maven Repository에 배포 (실제 배포)

**build.gradle.kts 주석 해제**:
```kotlin
publishing {
    repositories {
        maven {
            name = "CompanyRepo"
            url = uri("https://maven.your-company.com/releases")
            credentials {
                username = project.findProperty("maven.username") as String? ?: System.getenv("MAVEN_USERNAME")
                password = project.findProperty("maven.password") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
```

**배포 실행**:
```bash
# 자격증명 설정
export MAVEN_USERNAME=your-username
export MAVEN_PASSWORD=your-password

# 배포
./gradlew publish
```

또는 `~/.gradle/gradle.properties`에 저장:
```properties
maven.username=your-username
maven.password=your-password
```

---

## 다른 서비스에서 사용 방법

### Order Service (Producer 예시)

#### 1. build.gradle.kts에 의존성 추가

```kotlin
// order-service/build.gradle.kts

repositories {
    mavenCentral()
    mavenLocal()  // 로컬 테스트 시
    // 또는
    // maven {
    //     url = uri("https://maven.your-company.com/releases")
    // }
}

dependencies {
    // Avro 이벤트 클래스 의존성 추가
    implementation("com.c4ang:c4ang-avro-events:1.0.0-SNAPSHOT")

    // Kafka 및 Avro 의존성
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
}
```

#### 2. Kafka Producer 구현

```kotlin
// order-service/src/main/kotlin/com/c4ang/order/event/OrderEventPublisher.kt
package com.c4ang.order.event

import com.c4ang.events.order.OrderCreatedEvent  // ← Avro 클래스 import
import com.c4ang.events.common.EventMetadata
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, OrderCreatedEvent>
) {

    fun publishOrderCreated(orderId: String, customerId: String, productId: String, quantity: Int, totalAmount: Double) {
        val event = OrderCreatedEvent.newBuilder()
            .setMetadata(EventMetadata.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("OrderCreated")
                .setTimestamp(System.currentTimeMillis())
                .setCorrelationId(orderId)
                .setVersion("1.0")
                .setSource("order-service")
                .build())
            .setOrderId(orderId)
            .setCustomerId(customerId)
            .setProductId(productId)
            .setQuantity(quantity)
            .setTotalAmount(totalAmount.toBigDecimal().toByteBuffer())
            .setOrderStatus(OrderStatus.PENDING_PAYMENT)
            .build()

        kafkaTemplate.send("c4ang.order.created", orderId, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    println("✅ OrderCreatedEvent published: $orderId")
                } else {
                    println("❌ Failed to publish: ${ex.message}")
                }
            }
    }

    private fun BigDecimal.toByteBuffer(): java.nio.ByteBuffer {
        return java.nio.ByteBuffer.wrap(this.unscaledValue().toByteArray())
    }
}
```

#### 3. application.yml 설정

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

### Payment Service (Consumer 예시)

#### 1. build.gradle.kts에 의존성 추가

```kotlin
// payment-service/build.gradle.kts

dependencies {
    // Avro 이벤트 클래스 의존성 추가
    implementation("com.c4ang:c4ang-avro-events:1.0.0-SNAPSHOT")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
}
```

#### 2. Kafka Consumer 구현

```kotlin
// payment-service/src/main/kotlin/com/c4ang/payment/event/OrderEventListener.kt
package com.c4ang.payment.event

import com.c4ang.events.order.OrderCreatedEvent  // ← Avro 클래스 import
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class OrderEventListener {

    @KafkaListener(
        topics = ["c4ang.order.created"],
        groupId = "payment-service"
    )
    fun handleOrderCreated(event: OrderCreatedEvent) {
        val orderId = event.getOrderId()
        val totalAmount = event.getTotalAmount()

        println("📩 Received OrderCreatedEvent: orderId=$orderId")

        // 결제 처리 로직
        processPayment(orderId, totalAmount)
    }

    private fun processPayment(orderId: String, amount: java.nio.ByteBuffer) {
        // 결제 처리...
    }
}
```

#### 3. application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: payment-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://localhost:8081
        specific.avro.reader: true  # ← 중요: Avro 클래스 사용
```

---

## 배포된 Artifact 구조

### c4ang-avro-events JAR 내부

```
c4ang-avro-events-1.0.0-SNAPSHOT.jar
├── com/c4ang/events/
│   ├── common/
│   │   └── EventMetadata.class
│   └── order/
│       ├── OrderCreatedEvent.class
│       ├── OrderCreatedEvent$Builder.class
│       └── OrderStatus.class
└── avro/
    └── *.avsc (원본 스키마 포함)
```

**포함되는 클래스**:
- ✅ Avro 생성 Java/Kotlin 클래스
- ✅ Builder 클래스
- ✅ Enum 클래스
- ✅ 원본 .avsc 스키마 파일

**포함되지 않는 것**:
- ❌ Spring Boot 코드
- ❌ Contract 테스트 코드
- ❌ 빌드 스크립트

---

## 버전 관리 전략

### Semantic Versioning 사용

```
1.0.0-SNAPSHOT  → 개발 중
1.0.0           → 첫 정식 릴리스
1.1.0           → 새 필드 추가 (하위 호환)
2.0.0           → Breaking Change (하위 호환 불가)
```

### build.gradle.kts 버전 업데이트

```kotlin
group = "com.c4ang"
version = "1.1.0"  // 버전 변경
```

### Avro 스키마 호환성 체크

**하위 호환 가능 (MINOR 버전 UP)**:
- ✅ 새 필드 추가 (default 값 필수)
- ✅ 필드 주석 변경
- ✅ Enum에 새 값 추가

**하위 호환 불가능 (MAJOR 버전 UP)**:
- ❌ 필드 제거
- ❌ 필드 타입 변경
- ❌ 필수 필드 추가 (default 없이)
- ❌ Enum 값 제거

---

## CI/CD 파이프라인 통합

### GitHub Actions 예시

```yaml
# .github/workflows/publish-avro.yml
name: Publish Avro Artifacts

on:
  push:
    branches:
      - main
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Publish to Maven Repository
        env:
          MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}
        run: ./gradlew publish

      - name: Notify Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Avro artifacts published: ${{ github.ref }}'
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 개발 워크플로우

### 1. Avro 스키마 변경

```bash
# Contract Hub 프로젝트
vim src/main/avro/events/OrderCreatedEvent.avsc

# 변경 예시: 새 필드 추가
{
  "name": "deliveryAddress",
  "type": "string",
  "default": "",  // 하위 호환성을 위한 default 값 필수!
  "doc": "배송 주소"
}
```

### 2. 로컬 테스트

```bash
# Avro 클래스 생성 및 로컬 배포
./gradlew clean build publishToMavenLocal
```

### 3. 다른 서비스에서 테스트

```bash
# Order Service에서 새 버전 사용
cd ../order-service
./gradlew clean build

# 새 필드 사용
event.setDeliveryAddress("서울시 강남구...")
```

### 4. 정식 배포

```bash
# 버전 업데이트
# build.gradle.kts: version = "1.1.0"

# Maven Repository에 배포
./gradlew publish

# Git 태그
git tag v1.1.0
git push origin v1.1.0
```

### 5. 다른 서비스 버전 업데이트

```kotlin
// order-service/build.gradle.kts
dependencies {
    implementation("com.c4ang:c4ang-avro-events:1.1.0")  // 버전 업데이트
}
```

---

## 멀티 모듈 프로젝트 대안

현재는 단일 모듈이지만, 향후 다음과 같이 분리할 수도 있습니다:

```
c4ang-contract-hub/
├── contract-hub-avro/         # Avro 전용 모듈
│   ├── build.gradle.kts
│   └── src/main/avro/
├── contract-hub-stubs/         # Contract Stub 전용 모듈
│   ├── build.gradle.kts
│   └── src/contractTest/
└── settings.gradle.kts
```

**장점**:
- ✅ Avro 클래스만 의존성 추가 가능
- ✅ 더 작은 JAR 크기
- ✅ 빌드 캐시 효율성

**단점**:
- ❌ 프로젝트 구조 복잡도 증가
- ❌ 관리 포인트 증가

→ **현재는 단일 모듈로 충분**, 필요시 나중에 분리

---

## 트러블슈팅

### 문제 1: 다른 서비스에서 Avro 클래스를 찾을 수 없음

**증상**:
```
Could not resolve com.c4ang:c4ang-avro-events:1.0.0-SNAPSHOT
```

**해결책**:
1. Contract Hub에서 배포 확인:
   ```bash
   ./gradlew publishToMavenLocal
   ls ~/.m2/repository/com/c4ang/c4ang-avro-events/
   ```

2. 다른 서비스 build.gradle.kts에 repository 추가:
   ```kotlin
   repositories {
       mavenLocal()  // 추가
   }
   ```

3. Gradle 캐시 삭제 후 재시도:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

### 문제 2: Avro 클래스 버전 충돌

**증상**:
```
java.lang.NoSuchMethodError: com.c4ang.events.order.OrderCreatedEvent.getDeliveryAddress()
```

**원인**: Order Service가 구 버전의 Avro 클래스를 사용 중

**해결책**:
```kotlin
// order-service/build.gradle.kts
dependencies {
    // 버전을 명시적으로 최신으로 변경
    implementation("com.c4ang:c4ang-avro-events:1.1.0")
}
```

### 문제 3: Schema Registry 호환성 오류

**증상**:
```
Schema being registered is incompatible with an earlier schema
```

**원인**: Avro 스키마가 하위 호환되지 않음

**해결책**:
1. 하위 호환 가능하게 스키마 수정
2. 또는 Schema Registry에서 호환성 체크 비활성화 (비추천):
   ```bash
   curl -X PUT http://localhost:8081/config/c4ang.order.created-value \
     -H "Content-Type: application/json" \
     -d '{"compatibility": "NONE"}'
   ```

---

## 참고 자료

- [Apache Avro - Getting Started](https://avro.apache.org/docs/current/getting-started-java/)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Gradle Publishing](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Semantic Versioning](https://semver.org/)

---

## 요약

### Avro 클래스 배포

```bash
# Contract Hub에서
./gradlew publishToMavenLocal  # 로컬 테스트
./gradlew publish              # Maven Repository에 배포
```

### 다른 서비스에서 사용

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.c4ang:c4ang-avro-events:1.0.0-SNAPSHOT")
}
```

```kotlin
// Kotlin 코드
import com.c4ang.events.order.OrderCreatedEvent

val event = OrderCreatedEvent.newBuilder()
    .setOrderId("ORD-123")
    .build()
```

**단일 진실 공급원 (Single Source of Truth)**:
- Avro 스키마 (`.avsc`) → Avro 클래스 생성 → JAR 배포 → 모든 서비스에서 사용 ✅
