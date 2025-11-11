# C4ang Contract Hub

> Contract testing suite for the c4ang e-commerce microservices ecosystem

## 📋 프로젝트 개요

C4ang Contract Hub는 **Kotlin으로 개발된** MSA(Microservices Architecture) 환경에서 서비스 간 통신의 안정성과 일관성을 보장하기 위한 중앙 집중식 계약 관리 시스템입니다. Spring Cloud Contract의 Kotlin DSL을 활용하여 타입 안전한 계약 정의와 테스트를 제공합니다.

## 🛠️ 기술 스택

- **Contract Testing**: Spring Cloud Contract (Kotlin DSL)
- **Message Broker**: Apache Kafka
- **Serialization**: Apache Avro
- **Schema Registry**: Confluent Schema Registry
- **Build Tool**: Gradle 8.5 (Kotlin DSL)
- **Language**: Kotlin 1.9.21 + Java 17
- **Framework**: Spring Boot 3.2

## 🎯 핵심 책임

### 1. MSA 서비스 간 명세 관리 (Spring Cloud Contract)

Spring Cloud Contract를 활용하여 Producer-Consumer 간의 API 계약을 관리하고 검증합니다.

**주요 기능:**
- Producer 서비스의 API 명세를 Kotlin DSL로 정의
- Consumer 서비스를 위한 Stub 자동 생성
- Contract 기반 자동 테스트 생성 및 실행 (JUnit5)
- Gradle을 통한 Stub 배포 및 공유
- 타입 안전한 Contract 작성 (Kotlin 타입 시스템 활용)

**장점:**
- API 변경 시 Breaking Change 사전 감지
- Producer-Consumer 간 계약 불일치 방지
- 통합 테스트 없이도 서비스 간 호환성 검증
- 문서화와 테스트의 동기화

### 2. Choreography Saga 패턴의 이벤트 흐름 문서화

Choreography 방식의 Saga 패턴에서 Kafka를 통해 비동기적으로 전달되는 이벤트 흐름을 시각화하고 문서화합니다.

**주요 기능:**
- 비즈니스 플로우별 이벤트 체인 문서화
- Kafka 토픽 및 Avro 스키마 명세
- 이벤트 발행/구독 관계 정의
- 보상 트랜잭션(Compensation) 흐름 정의
- Saga 패턴 실패 시나리오 및 처리 방안

**장점:**
- 분산 트랜잭션 흐름의 가시성 확보
- 이벤트 기반 아키텍처의 복잡도 관리
- Avro 스키마를 통한 타입 안정성
- 장애 발생 시 디버깅 용이
- 신규 개발자의 시스템 이해도 향상

## 🏗️ 프로젝트 구조

```
c4ang-contract-hub/  (단일 모듈 프로젝트)
│
├── .github/
│   └── workflows/                      # GitHub Actions CI/CD
│       ├── pr-validation.yml           # PR 검증
│       ├── branch-build.yml            # 브랜치 빌드
│       └── release.yml                 # 릴리스 배포
│
├── src/
│   ├── main/
│   │   ├── kotlin/                    # 메인 소스 코드
│   │   ├── resources/                 # 리소스 파일
│   │   └── avro/                      # Avro 스키마 정의
│   │       ├── common/                # 공통 스키마
│   │       │   └── EventMetadata.avsc
│   │       └── events/                # 이벤트 스키마
│   │           ├── OrderCreatedEvent.avsc
│   │           ├── PaymentCompletedEvent.avsc
│   │           └── ...
│   │
│   ├── test/
│   │   ├── kotlin/                    # 테스트 코드
│   │   │   └── com/c4ang/contract/
│   │   │       └── BaseContractTest.kt
│   │   └── resources/                 # 테스트 리소스
│   │
│   └── contractTest/                  # Contract 전용 source set
│       ├── kotlin/                    # Contract 테스트 코드 (향후)
│       └── resources/
│           └── contracts/             # Spring Cloud Contract 명세
│               ├── README.md          # Contract 작성 가이드
│               ├── order-service/     # 주문 서비스 계약
│               ├── payment-service/   # 결제 서비스 계약
│               ├── inventory-service/ # 재고 서비스 계약
│               ├── notification-service/ # 알림 서비스 계약
│               └── messaging/         # Kafka 메시징 계약
│
├── event-flows/                        # 이벤트 흐름 문서
│   ├── README.md                      # 이벤트 흐름 가이드
│   ├── order-saga/                    # 주문 Saga 플로우
│   ├── payment-saga/                  # 결제 Saga 플로우 (예정)
│   └── diagrams/                      # 플로우 다이어그램
│
├── docs/                               # 상세 가이드라인
│   ├── quick-start-guide.md           # 시작 가이드 + IDE 설정
│   ├── jitpack-publishing-guide.md    # JitPack 배포 (토이 프로젝트)
│   ├── avro-artifact-publishing.md    # Avro 클래스 배포 및 사용
│   ├── avro-integration-strategy.md   # Avro 통합 전략
│   └── gradle-buildSrc-guide.md       # buildSrc 가이드
│
├── buildSrc/                           # 빌드 스크립트
│   └── src/main/kotlin/
│       └── AvroDocGenerator.kt        # Avro 문서 자동 생성
│
└── build/
    └── generated-main-avro-java/      # Avro 생성 클래스
```

## 🚀 시작하기

### 사전 요구사항

- JDK 17 이상
- Gradle 8.x
- Spring Boot 3.x

### 설치 및 실행

```bash
# 프로젝트 클론
git clone <repository-url>
cd c4ang-contract-hub

# Avro 스키마로부터 Java 클래스 생성
./gradlew generateAvroJava

# 의존성 설치 및 빌드
./gradlew build

# Contract 테스트 실행
./gradlew contractTest

# Stub 생성 및 로컬 배포
./gradlew publishToMavenLocal
```

### Avro 클래스 배포 (다른 서비스에서 사용)

#### JitPack 배포 (토이 프로젝트 권장)

```bash
# 1. build.gradle.kts에서 group 변경
# group = "com.github.your-username"

# 2. Git Tag 생성 및 Push
git tag v1.0.0
git push origin v1.0.0

# 3. JitPack 자동 빌드
# https://jitpack.io/#your-username/c4ang-contract-hub
```

#### 다른 서비스에서 사용 (Producer/Consumer)

```kotlin
// build.gradle.kts
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Avro 이벤트 클래스 가져오기
    implementation("com.github.your-username:c4ang-contract-hub:v1.0.0")
}
```

```kotlin
// Producer (Order Service)
import com.c4ang.events.order.OrderCreatedEvent

val event = OrderCreatedEvent.newBuilder()
    .setOrderId("ORD-123")
    .setCustomerId("CUST-001")
    .build()

kafkaTemplate.send("c4ang.order.created", orderId, event)
```

```kotlin
// Consumer (Payment Service)
import com.c4ang.events.order.OrderCreatedEvent

@KafkaListener(topics = ["c4ang.order.created"])
fun handleOrderCreated(event: OrderCreatedEvent) {
    val orderId = event.getOrderId()
    processPayment(orderId)
}
```

**상세 가이드**: [JitPack 배포 가이드](docs/publishing/jitpack-publishing-guide.md)

## 📖 사용 가이드

### 1. Spring Cloud Contract 작성

HTTP API의 Producer-Consumer 계약을 **Kotlin DSL**로 정의합니다.

**빠른 시작:**
1. `src/contractTest/resources/contracts/<service-name>` 디렉토리에 Kotlin Contract 파일 작성 (`.kts`)
2. Contract 테스트 실행으로 검증
3. Stub 생성 및 배포

**상세 가이드**: [src/contractTest/resources/contracts/README.md](src/contractTest/resources/contracts/README.md)

### 2. 이벤트 흐름 문서화

Kafka 기반 Saga 패턴의 이벤트 흐름을 문서화합니다.

**빠른 시작:**
1. `src/main/avro/events/` 에 Avro 스키마 정의
2. `event-flows/<saga-name>` 에 플로우 문서 작성
3. Kafka 토픽명 및 이벤트 명세 정의

**상세 가이드**: [event-flows/README.md](event-flows/README.md)

### 3. Avro 스키마 개발

**스키마 작성 후 Java 클래스 생성:**
```bash
./gradlew generateAvroJava
```

**생성된 클래스 위치:**
```
build/generated-main-avro-java/com/c4ang/events/
```

**주요 이벤트 스키마:**
- `OrderCreatedEvent` - 주문 생성
- `PaymentCompletedEvent` - 결제 완료
- `PaymentFailedEvent` - 결제 실패 (보상)
- `InventoryReservedEvent` - 재고 예약
- `InventoryReservationFailedEvent` - 재고 예약 실패 (보상)
- `PaymentCancelledEvent` - 결제 취소 (보상)

## 🔄 CI/CD 및 버전 관리

### GitHub Actions 자동화

이 프로젝트는 GitHub Actions를 통해 자동 빌드 및 배포를 수행합니다.

| Workflow | 트리거 | 동작 | Badge |
|----------|--------|------|-------|
| **PR Validation** | PR 생성/업데이트 | 빌드 + 테스트 | ![PR Validation](https://github.com/groom/c4ang-contract-hub/workflows/PR%20Validation/badge.svg) |
| **Branch Build** | develop/feature Push | 빌드 + 테스트 + JitPack 준비 | ![Branch Build](https://github.com/groom/c4ang-contract-hub/workflows/Branch%20Build/badge.svg) |
| **Release** | Tag Push (v*) | 빌드 + 테스트 + GitHub Release | ![Release](https://github.com/groom/c4ang-contract-hub/workflows/Release/badge.svg) |

### 브랜치별 버전 전략

**JitPack Branch 기반 버전 관리**를 사용합니다:

```bash
# Production (main)
git tag v1.0.0
git push origin v1.0.0
# → JitPack: 1.0.0

# Development (develop)
git push origin develop
# → JitPack: develop-SNAPSHOT

# Feature (feature/user-auth)
git push origin feature/user-auth
# → JitPack: feature-user-auth-SNAPSHOT
```

**Consumer/Producer에서 환경별 버전 사용:**

```kotlin
dependencies {
    // Production
    implementation("com.github.groom:c4ang-contract-hub:1.0.0")

    // Development
    // implementation("com.github.groom:c4ang-contract-hub:develop-SNAPSHOT")

    // Feature Test
    // implementation("com.github.groom:c4ang-contract-hub:feature-user-auth-SNAPSHOT")
}
```

**상세 가이드**: [버전 관리 전략](docs/versioning-strategy.md)

### 릴리스 프로세스

```bash
# 1. develop에서 기능 개발 및 테스트
git checkout develop
# ... 개발 ...

# 2. main으로 머지
git checkout main
git merge develop

# 3. 버전 태그 생성 및 Push
git tag v1.0.0
git push origin v1.0.0

# 4. GitHub Actions가 자동으로:
#    - 빌드 및 테스트 실행
#    - GitHub Release 생성
#    - JitPack 빌드 트리거
```

## 🔧 개발 워크플로우

### HTTP API Contract
1. **Contract 정의**: Producer 팀이 API Contract 작성 (Kotlin DSL `.kts`)
   - 위치: `src/contractTest/resources/contracts/<service-name>/`
2. **Stub 생성**: Contract로부터 Stub 자동 생성
3. **Consumer 개발**: Consumer 팀이 Stub을 사용하여 독립적 개발
4. **Contract 검증**: Producer의 실제 구현이 Contract를 만족하는지 테스트
5. **Stub 배포**: Maven/Gradle Repository에 Stub 배포

### Kafka 이벤트
1. **Avro 스키마 정의**: `src/main/avro/events/` 에 스키마 작성
2. **Java 클래스 생성**: Gradle 플러그인으로 자동 생성
3. **이벤트 문서화**: Saga 플로우 및 토픽 명세 작성
4. **Schema Registry 등록**: 스키마 버전 관리
5. **이벤트 구현**: Producer/Consumer 구현 및 테스트

## 📚 문서

### 시작하기
- **[Quick Start Guide](docs/quick-start-guide.md)** ⭐ - 프로젝트 사용법, 워크플로우, IDE 설정
- **[버전 관리 전략](docs/versioning-strategy.md)** 🔄 - Git Flow 브랜치 전략 및 JitPack 배포 가이드
- **[JitPack 배포 가이드](docs/publishing/jitpack-publishing-guide.md)** 🚀 - 토이 프로젝트를 위한 무료 배포 방법
- **[Avro Artifact 배포 가이드](docs/publishing/avro-artifact-publishing.md)** - 다른 서비스에서 Avro 클래스 사용하기
- **[Avro 통합 전략](docs/avro-integration-strategy.md)** - Avro 스키마 활용 및 문서 자동화 전략
- **[Gradle buildSrc 가이드](docs/gradle-buildSrc-guide.md)** - buildSrc를 활용한 커스텀 빌드 로직 구현

### 작성 가이드
- **[Contract 작성 가이드](src/contractTest/resources/contracts/README.md)** - Spring Cloud Contract 작성 방법 및 예시
- **[이벤트 흐름 가이드](event-flows/README.md)** - Kafka/Avro 기반 이벤트 흐름 문서화
- **[주문 Saga 플로우](event-flows/order-saga/README.md)** - 주문 생성부터 완료까지의 전체 플로우

### 자동 생성 문서
- **[이벤트 명세](docs/generated/event-specifications.md)** - Avro 스키마로부터 자동 생성된 전체 이벤트 명세

### 외부 문서
- [Spring Cloud Contract 공식 문서](https://spring.io/projects/spring-cloud-contract)
- [Apache Kafka 문서](https://kafka.apache.org/documentation/)
- [Apache Avro 문서](https://avro.apache.org/docs/current/)
- [Saga Pattern 가이드](https://microservices.io/patterns/data/saga.html)

## 🤝 기여 방법

### HTTP API Contract 추가
1. `src/contractTest/resources/contracts/<service-name>/` 디렉토리 생성
2. Kotlin DSL로 Contract 작성 (`.kts` 파일)
3. 테스트 실행 및 검증
4. Pull Request 제출

### 이벤트 Saga 플로우 추가
1. `src/main/avro/events/` 에 Avro 스키마 정의
2. `event-flows/<saga-name>/` 디렉토리 생성
3. README.md에 플로우 문서 작성 (Mermaid 다이어그램 포함)
4. Kafka 토픽명 및 이벤트 명세 정의
5. Pull Request 제출

### 주의사항
- Contract 변경 시 관련 팀과 사전 협의
- Avro 스키마 변경 시 하위 호환성 검토
- 보상 트랜잭션을 포함한 실패 시나리오 문서화 필수

## 📄 라이선스

Copyright (c) 2025 C4ang Team
