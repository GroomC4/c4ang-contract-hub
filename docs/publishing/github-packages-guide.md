# GitHub Packages를 활용한 Avro 클래스 배포

## GitHub Packages란?

**GitHub Packages**는 GitHub에서 제공하는 패키지 호스팅 서비스로, Maven, npm, Docker 등 다양한 패키지 형식을 지원합니다.

---

## 왜 GitHub Packages인가?

### Organization 레벨 통일

GroomC4 Organization에서는 모든 프로젝트의 패키지 배포를 **GitHub Packages**로 통일합니다.

### 비교표

| 항목 | GitHub Packages | JitPack | Maven Central |
|------|-----------------|---------|---------------|
| **설정 난이도** | ⭐⭐ 쉬움 | ⭐ 매우 쉬움 | ⭐⭐⭐⭐⭐ 매우 어려움 |
| **비용** | **무료** | 무료 | 무료 |
| **비공개 저장소** | ✅ 지원 | ❌ 유료 | N/A |
| **인증 필요** | ✅ GitHub Token | ❌ 불필요 | ✅ 복잡한 인증 |
| **Organization 통합** | ✅ **최적** | ⚠️ 제한적 | ✅ 가능 |
| **CI/CD 통합** | ✅ GitHub Actions 네이티브 | ✅ 가능 | ✅ 가능 |
| **패키지 관리** | ✅ GitHub UI에서 관리 | ❌ 별도 | ❌ 별도 |

### 결론

**Organization 프로젝트는 GitHub Packages가 최적**
- GitHub Actions와 완벽한 통합
- 중앙 패키지 허브(c4ang-packages-hub)로 통합 관리
- 비공개 저장소도 무료로 지원

---

## GitHub Packages 장점

- ✅ **GitHub 네이티브 통합** - Actions, Issues, PR과 연동
- ✅ **Organization 레벨 관리** - 중앙 집중식 패키지 관리
- ✅ **비공개 저장소 지원** - 무료로 비공개 패키지 배포
- ✅ **자동 배포** - GitHub Actions에서 GITHUB_TOKEN으로 인증
- ✅ **버전 관리** - GitHub UI에서 패키지 버전 확인 및 삭제

## GitHub Packages 단점

- ⚠️ 사용 시 GitHub Token 인증 필요
- ⚠️ 공개 패키지도 다운로드 시 인증 필요

---

## 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    GroomC4 Organization                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐    publish    ┌──────────────────┐   │
│  │ c4ang-contract-  │ ──────────────▶│ c4ang-packages-  │   │
│  │ hub              │               │ hub              │   │
│  └──────────────────┘               │ (중앙 패키지 허브)│   │
│                                     └────────┬─────────┘   │
│  ┌──────────────────┐    publish             │             │
│  │ order-service    │ ───────────────────────┤             │
│  └──────────────────┘                        │             │
│                                              │             │
│  ┌──────────────────┐    publish             │             │
│  │ payment-service  │ ───────────────────────┤             │
│  └──────────────────┘                        │             │
│                                              ▼             │
│                                    ┌──────────────────┐   │
│                                    │ 모든 서비스에서   │   │
│                                    │ 의존성으로 사용   │   │
│                                    └──────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 설정 방법

### 1. build.gradle.kts 설정

```kotlin
plugins {
    java
    kotlin("jvm") version "1.9.21"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("maven-publish")
}

// GitHub Packages 배포를 위한 group 설정
group = "io.github.groomc4"  // io.github.{조직명_소문자}
version = "1.1.0"

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "c4ang-contract-hub"
            version = project.version.toString()

            pom {
                name.set("C4ANG Contract Hub")
                description.set("동기/비동기 통신 스키마 통합 관리 라이브러리")
                url.set("https://github.com/GroomC4/c4ang-contract-hub")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("GroomC4")
                        name.set("GroomC4 Team")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/GroomC4/c4ang-contract-hub.git")
                    developerConnection.set("scm:git:ssh://github.com/GroomC4/c4ang-contract-hub.git")
                    url.set("https://github.com/GroomC4/c4ang-contract-hub")
                }
            }
        }
    }

    repositories {
        // Maven Local (로컬 테스트용)
        mavenLocal()

        // GitHub Packages (중앙 패키지 허브로 배포)
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**중요**:
- `group`을 `io.github.{조직명_소문자}` 형식으로 설정
- 배포 대상은 중앙 패키지 허브 `c4ang-packages-hub`

### 2. 로컬 개발 환경 설정

`~/.gradle/gradle.properties` 파일에 인증 정보 추가:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

GitHub Token 생성 방법:
1. GitHub → Settings → Developer settings → Personal access tokens
2. "Generate new token (classic)" 클릭
3. 필요한 권한 선택:
   - `read:packages` - 패키지 다운로드
   - `write:packages` - 패키지 업로드 (배포 시)
   - `delete:packages` - 패키지 삭제 (선택)

### 3. GitHub Actions 설정

`.github/workflows/release.yml`:

```yaml
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

**참고**: Organization 레벨 토큰 `GROOM_GITHUB_ACTION_TOKEN` 사용

---

## 배포 워크플로우

### 자동 배포 (권장)

```bash
# 1. 버전 업데이트 (build.gradle.kts)
# version = "1.2.0"

# 2. 변경사항 커밋
git add .
git commit -m "chore: version bump to 1.2.0"
git push origin main

# 3. 태그 생성 및 Push
git tag v1.2.0
git push origin v1.2.0

# 4. GitHub Actions가 자동으로 배포
# https://github.com/GroomC4/c4ang-contract-hub/actions
```

### 수동 배포 (로컬)

```bash
# 환경 변수 설정
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxx

# 배포
./gradlew publish
```

---

## 다른 서비스에서 사용 방법

### 1. build.gradle.kts 설정

```kotlin
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

### 2. Kotlin 코드에서 사용

```kotlin
import com.groom.ecommerce.order.event.avro.OrderCreated

val event = OrderCreated.newBuilder()
    .setEventId(UUID.randomUUID().toString())
    .setOrderId("ORD-123")
    .build()
```

---

## CI/CD 환경 설정

### GitHub Actions에서 의존성 다운로드

다른 서비스의 GitHub Actions에서 Contract Hub를 의존성으로 사용할 때:

```yaml
# .github/workflows/build.yml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**참고**: `GITHUB_TOKEN`은 GitHub Actions에서 자동으로 제공됩니다.

---

## 버전 관리

### Semantic Versioning

```
1.0.0        → 첫 정식 릴리스
1.1.0        → 새 필드 추가 (하위 호환)
2.0.0        → Breaking Change (하위 호환 불가)
```

### 패키지 버전 확인

GitHub UI에서 확인:
- https://github.com/orgs/GroomC4/packages

또는 Gradle로 확인:
```bash
./gradlew dependencies --configuration runtimeClasspath | grep c4ang
```

---

## 트러블슈팅

### 문제 1: 401 Unauthorized

**증상**:
```
Could not resolve io.github.groomc4:c4ang-contract-hub:1.1.0
Received status code 401 from server: Unauthorized
```

**해결책**:
1. GitHub Token 확인:
   ```bash
   echo $GITHUB_TOKEN
   ```

2. Token 권한 확인:
   - `read:packages` 권한이 있는지 확인

3. `~/.gradle/gradle.properties` 확인:
   ```properties
   gpr.user=your-username
   gpr.key=ghp_xxxxxxxxxxxxx
   ```

### 문제 2: 패키지를 찾을 수 없음

**증상**:
```
Could not find io.github.groomc4:c4ang-contract-hub:1.1.0
```

**해결책**:
1. 패키지가 배포되었는지 확인:
   - https://github.com/orgs/GroomC4/packages

2. Repository URL 확인:
   ```kotlin
   url = uri("https://maven.pkg.github.com/GroomC4/c4ang-packages-hub")
   ```

3. Gradle 캐시 삭제:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

### 문제 3: 배포 실패

**증상**:
```
Could not PUT 'https://maven.pkg.github.com/...'
Received status code 403 from server: Forbidden
```

**해결책**:
1. Token에 `write:packages` 권한 확인
2. Organization 토큰 사용 확인 (`GROOM_GITHUB_ACTION_TOKEN`)
3. 대상 저장소에 쓰기 권한 확인

---

## JitPack에서 마이그레이션

### 변경 전 (JitPack)

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.GroomC4:c4ang-contract-hub:v1.0.0")
}
```

### 변경 후 (GitHub Packages)

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

dependencies {
    implementation("io.github.groomc4:c4ang-contract-hub:1.1.0")
}
```

**주요 변경사항**:
- Group ID: `com.github.GroomC4` → `io.github.groomc4`
- 버전 형식: `v1.0.0` → `1.1.0` (v 접두사 제거)
- 인증 필요: GitHub Token 설정 필요

---

## 참고 자료

- [GitHub Packages 공식 문서](https://docs.github.com/en/packages)
- [Gradle Publishing to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
- [GitHub Actions와 Packages 통합](https://docs.github.com/en/packages/managing-github-packages-using-github-actions-workflows)

---

## 요약

### 배포 (Contract Hub)

```bash
# 버전 업데이트 후 태그 생성
git tag v1.2.0
git push origin v1.2.0
# → GitHub Actions가 자동 배포
```

### 사용 (다른 서비스)

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
