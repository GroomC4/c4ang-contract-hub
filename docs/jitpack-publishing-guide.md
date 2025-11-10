# JitPack을 활용한 Avro 클래스 배포

## JitPack이란?

**JitPack**은 GitHub 저장소를 Maven Repository로 변환해주는 무료 서비스입니다.

---

## 왜 Maven Central이 아니라 JitPack인가?

### 비교표

| 항목          | JitPack      | Maven Central                 | 회사 Nexus/Artifactory |
|-------------|--------------|-------------------------------|----------------------|
| **설정 난이도**  | ⭐ 매우 쉬움      | ⭐⭐⭐⭐⭐ 매우 어려움                  | ⭐⭐⭐ 보통               |
| **설정 시간**   | **5분**       | 수일 ~ 수주                       | 수시간                  |
| **비용**      | **무료**       | 무료                            | 유료 (서버 구축)           |
| **필요 조건**   | GitHub 저장소   | Sonatype 계정, GPG 키, 도메인 소유 증명 | 회사 인프라               |
| **배포 방법**   | Git Tag Push | 복잡한 인증 + 수동 승인                | 자동화 가능               |
| **승인 절차**   | 없음 (즉시)      | **24~48시간** 대기                | 없음                   |
| **토이 프로젝트** | ✅ **최적**     | ❌ 과함                          | ❌ 불필요                |
| **상용 프로젝트** | ⚠️ 제한적       | ✅ 권장                          | ✅ 권장                 |

### 결론
**토이 프로젝트는 JitPack이 압도적으로 유리**

---

## JitPack 장점

- ✅ **완전 무료**
- ✅ **설정 매우 간단** (GitHub + Git Tag만 있으면 됨)
- ✅ **별도 서버 구축 불필요**
- ✅ **자동 빌드** (Tag 푸시 시 자동으로 JAR 생성)
- ✅ **Maven/Gradle에서 바로 사용 가능**

## JitPack 단점

- ⚠️ 공개 저장소만 가능 (비공개는 유료)
- ⚠️ 첫 빌드 시 시간 소요 (캐시되면 빠름)

---

## 설정 방법 (5분 컷)

### 1. build.gradle.kts 수정

JitPack은 자동으로 프로젝트를 빌드하므로 별도 publishing 설정이 거의 불필요합니다.

```kotlin
// build.gradle.kts

plugins {
    // ... 기존 플러그인
    `maven-publish`  // 이것만 있으면 됨
}

group = "com.github.your-username"  // ← 중요: com.github.{GitHub_유저명}
version = "1.0.0"  // Tag 이름과 일치시키는 것을 권장

// JitPack은 기본 java component를 자동으로 배포
// 별도 publishing 블록 불필요!
```

**중요**: `group`을 `com.github.{GitHub_유저명}` 형식으로 설정해야 합니다.

### 2. GitHub에 Push

```bash
git add .
git commit -m "Add JitPack support"
git push origin main
```

### 3. Git Tag 생성 및 Push

```bash
# Tag 생성
git tag v1.0.0

# Tag를 GitHub에 Push
git push origin v1.0.0
```

### 4. JitPack에서 빌드 트리거

브라우저에서 https://jitpack.io 접속:

1. GitHub 저장소 URL 입력: `https://github.com/your-username/c4ang-contract-hub`
2. "Look Up" 클릭
3. `v1.0.0` Tag 찾기
4. "Get It" 버튼 클릭 → 자동 빌드 시작

**또는 직접 URL 접속**:
```
https://jitpack.io/#your-username/c4ang-contract-hub/v1.0.0
```

빌드 로그를 실시간으로 볼 수 있습니다.

---

## 다른 프로젝트에서 사용 방법

### Order Service (Producer)

#### build.gradle.kts

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }  // ← JitPack 추가
}

dependencies {
    // JitPack 형식: com.github.{유저명}:{저장소명}:{버전}
    implementation("com.github.your-username:c4ang-contract-hub:v1.0.0")

    // Kafka & Avro
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
}
```

#### Kotlin 코드

```kotlin
import com.c4ang.events.order.OrderCreatedEvent  // ← JitPack에서 받아온 클래스

val event = OrderCreatedEvent.newBuilder()
    .setOrderId("ORD-123")
    .build()
```

---

## 실전 예시

### c4ang-contract-hub 프로젝트

#### 1. build.gradle.kts 설정

```kotlin
plugins {
    java
    kotlin("jvm") version "1.9.21"
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springframework.cloud.contract") version "4.1.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    `maven-publish`  // ← 추가
}

group = "com.github.groom"  // ← GitHub 유저명으로 변경
version = "1.0.0"

// 나머지 설정은 그대로...
```

#### 2. GitHub에 Push 및 Tag 생성

```bash
git add build.gradle.kts
git commit -m "Add JitPack support"
git push origin main

# Tag 생성 및 Push
git tag v1.0.0
git push origin v1.0.0
```

#### 3. JitPack 빌드 확인

https://jitpack.io/#groom/c4ang-contract-hub

빌드 성공하면:
- ✅ Status: `OK`
- ✅ Artifact: `c4ang-contract-hub-1.0.0.jar` 다운로드 가능

---

### order-service에서 사용

#### build.gradle.kts

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // JitPack에서 가져오기
    implementation("com.github.groom:c4ang-contract-hub:v1.0.0")
}
```

#### OrderEventPublisher.kt

```kotlin
package com.c4ang.order.event

import com.c4ang.events.order.OrderCreatedEvent  // ✅ 사용 가능!
import com.c4ang.events.common.EventMetadata
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, OrderCreatedEvent>
) {
    fun publishOrderCreated(orderId: String) {
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
            .setCustomerId("CUST-001")
            .setProductId("PROD-001")
            .setQuantity(2)
            .setTotalAmount(BigDecimal("50000").toByteBuffer())
            .setOrderStatus(OrderStatus.PENDING_PAYMENT)
            .build()

        kafkaTemplate.send("c4ang.order.created", orderId, event)
    }
}
```

---

## 버전 관리 워크플로우

### Avro 스키마 변경 시

```bash
# 1. Avro 스키마 수정
vim src/main/avro/events/OrderCreatedEvent.avsc

# 2. 버전 업데이트
vim build.gradle.kts
# version = "1.1.0"

# 3. Git 커밋 및 Tag
git add .
git commit -m "feat: Add deliveryAddress field to OrderCreatedEvent"
git push origin main

git tag v1.1.0
git push origin v1.1.0

# 4. JitPack 자동 빌드 (약 1-2분 소요)
# https://jitpack.io/#groom/c4ang-contract-hub/v1.1.0

# 5. 다른 서비스에서 버전 업데이트
# build.gradle.kts: implementation("com.github.groom:c4ang-contract-hub:v1.1.0")
```

---

## JitPack Badge 추가 (선택 사항)

README.md에 JitPack 버전 뱃지를 추가하면 멋집니다:

```markdown
# C4ang Contract Hub

[![](https://jitpack.io/v/groom/c4ang-contract-hub.svg)](https://jitpack.io/#groom/c4ang-contract-hub)

> Contract testing suite for the c4ang e-commerce microservices ecosystem
```

결과:
![JitPack Badge](https://jitpack.io/v/groom/c4ang-contract-hub.svg)

---

## JitPack 빌드 로그 확인

빌드가 실패하면 로그에서 원인 확인 가능:

https://jitpack.io/com/github/groom/c4ang-contract-hub/v1.0.0/build.log

일반적인 빌드 실패 원인:
1. ❌ `group` 설정이 `com.github.{유저명}` 형식이 아님
2. ❌ Java/Kotlin 버전 불일치
3. ❌ 의존성 다운로드 실패
4. ❌ 테스트 실패 (→ `./gradlew build -x test` 설정 필요)

---

## 대안: Gradle Composite Build (로컬 개발용)

Maven Repository 없이 로컬에서만 개발할 경우, Composite Build를 사용할 수 있습니다.

### 프로젝트 구조

```
workspace/
├── c4ang-contract-hub/     # Avro 클래스 정의
└── order-service/          # Producer
```

### order-service/settings.gradle.kts

```kotlin
rootProject.name = "order-service"

// c4ang-contract-hub를 includeBuild로 포함
includeBuild("../c4ang-contract-hub") {
    dependencySubstitution {
        substitute(module("com.c4ang:c4ang-contract-hub"))
            .using(project(":"))
    }
}
```

### order-service/build.gradle.kts

```kotlin
dependencies {
    // 일반 의존성처럼 사용
    implementation("com.c4ang:c4ang-contract-hub")
}
```

**장점**:
- ✅ 별도 배포 불필요
- ✅ 실시간 변경 사항 반영
- ✅ 로컬 개발에 최적

**단점**:
- ❌ 프로젝트를 함께 clone 해야 함
- ❌ CI/CD 환경에서는 사용 불가

---

## 대안: GitHub Packages (GitHub 통합)

GitHub에서 제공하는 무료 Maven Repository 서비스입니다.

### build.gradle.kts 설정

```kotlin
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/groom/c4ang-contract-hub")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")  // Personal Access Token
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
```

### 배포

```bash
export GITHUB_USERNAME=groom
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxx  # Personal Access Token 발급 필요

./gradlew publish
```

### 다른 서비스에서 사용

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/groom/c4ang-contract-hub")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.c4ang:c4ang-contract-hub:1.0.0")
}
```

**장점**:
- ✅ GitHub 통합
- ✅ 무료
- ✅ 공개/비공개 저장소 모두 지원

**단점**:
- ❌ 사용할 때마다 GitHub Token 필요 (번거로움)
- ❌ JitPack보다 설정 복잡

---

## 비교표

| | JitPack | GitHub Packages | Maven Central | Composite Build |
|---|---------|-----------------|---------------|-----------------|
| **비용** | 무료 | 무료 | 무료 | 무료 |
| **설정 난이도** | ⭐ 매우 쉬움 | ⭐⭐ 쉬움 | ⭐⭐⭐⭐⭐ 어려움 | ⭐ 매우 쉬움 |
| **배포 방법** | Git Tag만 | GitHub Token 필요 | 복잡한 인증 | 배포 불필요 |
| **사용 편의성** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **CI/CD 통합** | ✅ | ✅ | ✅ | ❌ |
| **비공개 저장소** | ❌ (유료) | ✅ | N/A | ✅ |
| **토이 프로젝트 추천** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐ |

---

## 권장 사항

### 토이 프로젝트라면

**1순위: JitPack** 🥇
- Git Tag만 푸시하면 끝
- 다른 사람도 쉽게 사용 가능
- 포트폴리오로 공유 시 유용

**2순위: Composite Build**
- 혼자 개발할 때
- 로컬 개발만 할 때

### 실무 프로젝트라면

**1순위: 회사 Nexus/Artifactory**
**2순위: GitHub Packages** (GitHub 사용 시)

---

## JitPack 빠른 시작 체크리스트

- [ ] build.gradle.kts에서 `group = "com.github.your-username"` 설정
- [ ] build.gradle.kts에서 `maven-publish` 플러그인 추가
- [ ] GitHub에 Push
- [ ] Git Tag 생성: `git tag v1.0.0`
- [ ] Tag Push: `git push origin v1.0.0`
- [ ] https://jitpack.io에서 빌드 확인
- [ ] 다른 프로젝트에서 테스트

---

## 참고 자료

- [JitPack 공식 문서](https://jitpack.io/docs/)
- [GitHub Packages 문서](https://docs.github.com/en/packages)
- [Gradle Composite Build](https://docs.gradle.org/current/userguide/composite_builds.html)

---

## 요약

### 가장 간단한 방법: JitPack

```bash
# 1. build.gradle.kts 설정
group = "com.github.groom"
version = "1.0.0"

# 2. Git Tag 생성 및 Push
git tag v1.0.0
git push origin v1.0.0

# 3. JitPack에서 자동 빌드
# https://jitpack.io/#groom/c4ang-contract-hub

# 4. 다른 프로젝트에서 사용
repositories {
    maven { url = uri("https://jitpack.io") }
}
dependencies {
    implementation("com.github.groom:c4ang-contract-hub:v1.0.0")
}
```

끝! 🎉
