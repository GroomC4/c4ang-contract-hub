# Avro 클래스 배포 및 사용 가이드

## 개요

이 프로젝트는 **Avro 스키마 기반 Java 클래스**를 배포합니다:

- **이벤트 스키마**: Kafka 비동기 통신용 (23개 이벤트)
- **API 스키마**: HTTP 동기 통신용 (4개 API)

다른 서비스(Producer, Consumer)는 이 Avro 클래스를 의존성으로 추가하여 사용할 수 있습니다.

**배포 방식**: GitHub Packages (Organization 레벨 통일)

---

## 배포 방법

### 1. Maven Local에 배포 (로컬 테스트)

```bash
# Contract Hub 프로젝트에서
./gradlew publishToMavenLocal
```

**결과**:
```
~/.m2/repository/io/github/groomc4/
└── c4ang-contract-hub/
    └── 1.1.0/
        ├── c4ang-contract-hub-1.1.0.jar       ← Avro 클래스
        └── c4ang-contract-hub-1.1.0-sources.jar
```

### 2. GitHub Packages에 배포 (실제 배포)

**자동 배포 (권장)**:
```bash
# 버전 업데이트 후 태그 생성
git tag v1.1.0
git push origin v1.1.0
# → GitHub Actions가 자동으로 GitHub Packages에 배포
```

**수동 배포**:
```bash
# 자격증명 설정
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxx

# 배포
./gradlew publish
```

또는 `~/.gradle/gradle.properties`에 저장:
```properties
gpr.user=your-username
gpr.key=ghp_xxxxxxxxxxxxx
```

**상세 가이드**: [GitHub Packages 배포 가이드](github-packages-guide.md)

---

## 다른 서비스에서 사용 방법

### Order Service (Producer 예시)

#### 1. build.gradle.kts에 의존성 추가

```kotlin
// order-service/build.gradle.kts

repositories {
    mavenCentral()
    // GitHub Packages (중앙 패키지 허브)
    maven {
        url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Contract Hub: 이벤트 + API 스키마
    implementation("io.github.groomc4:c4ang-contract-hub:1.1.0")

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

repositories {
    mavenCentral()
    // GitHub Packages (중앙 패키지 허브)
    maven {
        url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Contract Hub: 이벤트 + API 스키마
    implementation("io.github.groomc4:c4ang-contract-hub:1.1.0")

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

### c4ang-contract-hub JAR 내부

```
c4ang-contract-hub-1.1.0.jar
├── com/groom/ecommerce/
│   ├── order/event/avro/
│   │   ├── OrderCreated.class
│   │   ├── OrderCreated$Builder.class
│   │   └── OrderStatus.class
│   ├── payment/event/avro/
│   │   └── PaymentCompleted.class
│   ├── customer/api/avro/
│   │   └── UserInternalResponse.class
│   └── common/
│       ├── event/avro/EventMetadata.class
│       └── api/avro/ErrorResponse.class
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
1.0.0           → 첫 정식 릴리스
1.1.0           → 새 필드 추가 (하위 호환)
2.0.0           → Breaking Change (하위 호환 불가)
```

### build.gradle.kts 버전 업데이트

```kotlin
group = "io.github.groomc4"
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
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Publish to GitHub Packages
        run: ./gradlew publish
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GROOM_GITHUB_ACTION_TOKEN }}
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
# build.gradle.kts: version = "1.2.0"

# Git 커밋 및 태그
git add .
git commit -m "chore: version bump to 1.2.0"
git push origin main

git tag v1.2.0
git push origin v1.2.0

# → GitHub Actions가 자동으로 GitHub Packages에 배포
```

### 5. 다른 서비스 버전 업데이트

```kotlin
// order-service/build.gradle.kts
dependencies {
    implementation("io.github.groomc4:c4ang-contract-hub:1.2.0")  // 버전 업데이트
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
Could not resolve io.github.groomc4:c4ang-contract-hub:1.1.0
```

**해결책**:
1. GitHub Packages에서 배포 확인:
   - https://github.com/orgs/GroomC4/packages

2. 다른 서비스 build.gradle.kts에 repository 추가:
   ```kotlin
   repositories {
       maven {
           url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
           credentials {
               username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
               password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
           }
       }
   }
   ```

3. Gradle 캐시 삭제 후 재시도:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

### 문제 2: Avro 클래스 버전 충돌

**증상**:
```
java.lang.NoSuchMethodError: com.groom.ecommerce.order.event.avro.OrderCreated.getDeliveryAddress()
```

**원인**: Order Service가 구 버전의 Avro 클래스를 사용 중

**해결책**:
```kotlin
// order-service/build.gradle.kts
dependencies {
    // 버전을 명시적으로 최신으로 변경
    implementation("io.github.groomc4:c4ang-contract-hub:1.1.0")
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

# 태그 생성 시 GitHub Actions가 자동 배포
git tag v1.1.0
git push origin v1.1.0
```

### 다른 서비스에서 사용

```kotlin
// build.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.groomc4:c4ang-contract-hub:1.1.0")
}
```

```kotlin
// Kotlin 코드
import com.groom.ecommerce.order.event.avro.OrderCreated

val event = OrderCreated.newBuilder()
    .setOrderId("ORD-123")
    .build()
```

**단일 진실 공급원 (Single Source of Truth)**:
- Avro 스키마 (`.avsc`) → Avro 클래스 생성 → GitHub Packages 배포 → 모든 서비스에서 사용 ✅
