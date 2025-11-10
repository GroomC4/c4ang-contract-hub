# 버전 관리 및 배포 전략

## 개요

이 프로젝트는 **JitPack Branch 기반 버전 관리 전략**을 사용합니다.
- Git Flow (main-develop-feature) 브랜치 전략
- JitPack을 통한 무료 Artifact 배포
- 브랜치명을 버전으로 활용하여 관리 단순화

---

## 🎯 버전 전략

### 브랜치별 버전

| 브랜치 | 버전 형식 | 예시 | 배포 위치 | 사용처 |
|--------|----------|------|----------|--------|
| **main** (Tag) | `v{major}.{minor}.{patch}` | `v1.0.0`, `v1.1.0` | JitPack (Tag) | **Production 환경** |
| **develop** | `develop-SNAPSHOT` | `develop-SNAPSHOT` | JitPack (Branch) | **Development 환경** |
| **feature/\*** | `feature-{name}-SNAPSHOT` | `feature-user-auth-SNAPSHOT` | JitPack (Branch) | **Feature 테스트** |

### 버전 의미
- **정식 릴리스 (main)**: Semantic Versioning (v1.0.0, v1.1.0, v2.0.0)
- **개발 버전 (develop)**: `develop-SNAPSHOT` (다음 릴리스 개발 중)
- **기능 개발 버전 (feature)**: `feature-xxx-SNAPSHOT` (특정 기능 개발 중)
- **SNAPSHOT**: 불안정한 개발 버전임을 명시

---

## 📦 JitPack 사용 방법

### JitPack URL 구조

```
https://jitpack.io/#groom/c4ang-contract-hub

Tag:            https://jitpack.io/#groom/c4ang-contract-hub/v1.0.0
Branch:         https://jitpack.io/#groom/c4ang-contract-hub/develop-SNAPSHOT
Commit:         https://jitpack.io/#groom/c4ang-contract-hub/{commit-hash}
```

### JitPack 동작 방식

1. **Tag Push 시**: JitPack이 자동으로 해당 Tag를 빌드하여 캐시
2. **Branch Push 시**: JitPack이 브랜치 최신 커밋을 캐시 (요청 시 자동 빌드)
3. **첫 요청 시**: 빌드 시간 소요 (약 1-3분), 이후 캐시 사용

---

## 🚀 배포 워크플로우

### 1. Feature 개발

```bash
# feature 브랜치 생성
git checkout develop
git checkout -b feature/user-authentication

# 개발 및 커밋
git add .
git commit -m "feat: Add user authentication"
git push origin feature/user-authentication
```

**배포 결과**:
- GitHub Actions: 빌드 + 테스트 실행
- JitPack: 브랜치 자동 캐시 (첫 요청 시 빌드)

**Consumer/Producer에서 사용**:
```kotlin
dependencies {
    implementation("com.github.groom:c4ang-contract-hub:feature-user-authentication-SNAPSHOT")
}
```

### 2. Develop 통합

```bash
# feature → develop PR 생성 및 머지
git checkout develop
git merge feature/user-authentication
git push origin develop
```

**배포 결과**:
- GitHub Actions: 빌드 + 테스트 실행
- JitPack: develop 브랜치 자동 업데이트

**Consumer/Producer에서 사용**:
```kotlin
dependencies {
    implementation("com.github.groom:c4ang-contract-hub:develop-SNAPSHOT")
}
```

### 3. Production 릴리스

```bash
# develop → main PR 생성 및 머지
git checkout main
git merge develop

# Git Tag 생성 및 Push
git tag v1.0.0
git push origin v1.0.0
```

**배포 결과**:
- GitHub Actions: 빌드 + 테스트 실행
- JitPack: Tag 버전 자동 빌드

**Consumer/Producer에서 사용**:
```kotlin
dependencies {
    implementation("com.github.groom:c4ang-contract-hub:1.0.0")  // v 제외
}
```

---

## 🏗️ Consumer/Producer 프로젝트 설정

### Gradle 설정 (build.gradle.kts)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }  // JitPack 저장소 추가
    maven { url = uri("https://packages.confluent.io/maven/") }  // Kafka Avro
}

dependencies {
    // 환경별로 버전 선택
    implementation("com.github.groom:c4ang-contract-hub:1.0.0")  // Production
    // implementation("com.github.groom:c4ang-contract-hub:develop-SNAPSHOT")  // Development
    // implementation("com.github.groom:c4ang-contract-hub:feature-xxx-SNAPSHOT")  // Feature

    // Kafka & Avro
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
}
```

### 환경별 버전 관리 전략

#### 방법 1: Gradle Profile (권장)

```kotlin
// build.gradle.kts

val contractHubVersion = when (project.findProperty("env") as String? ?: "prod") {
    "prod" -> "1.0.0"
    "dev" -> "develop-SNAPSHOT"
    "feature" -> "feature-user-auth-SNAPSHOT"
    else -> "1.0.0"
}

dependencies {
    implementation("com.github.groom:c4ang-contract-hub:$contractHubVersion")
}
```

**사용법**:
```bash
# Production 빌드
./gradlew build -Penv=prod

# Development 빌드
./gradlew build -Penv=dev

# Feature 빌드
./gradlew build -Penv=feature
```

#### 방법 2: 환경 변수

```kotlin
// build.gradle.kts

val contractHubVersion = System.getenv("CONTRACT_HUB_VERSION") ?: "1.0.0"

dependencies {
    implementation("com.github.groom:c4ang-contract-hub:$contractHubVersion")
}
```

**사용법**:
```bash
# Development 환경
export CONTRACT_HUB_VERSION=develop-SNAPSHOT
./gradlew build

# Production 환경
export CONTRACT_HUB_VERSION=1.0.0
./gradlew build
```

#### 방법 3: Spring Profile

```yaml
# application-prod.yml
contract:
  hub:
    version: 1.0.0

# application-dev.yml
contract:
  hub:
    version: develop-SNAPSHOT
```

```kotlin
// build.gradle.kts
val contractHubVersion = project.findProperty("contract.hub.version") as String? ?: "1.0.0"
```

---

## 🔄 버전 업그레이드 가이드

### Consumer/Producer에서 버전 변경

#### Production → Development 전환

```kotlin
// Before (Production)
implementation("com.github.groom:c4ang-contract-hub:1.0.0")

// After (Development)
implementation("com.github.groom:c4ang-contract-hub:develop-SNAPSHOT")
```

```bash
# Gradle 캐시 갱신
./gradlew build --refresh-dependencies
```

#### Development → New Release 전환

```kotlin
// Before (Development)
implementation("com.github.groom:c4ang-contract-hub:develop-SNAPSHOT")

// After (New Release)
implementation("com.github.groom:c4ang-contract-hub:1.1.0")
```

---

## 🐛 트러블슈팅

### 문제 1: JitPack 빌드 실패

**증상**:
```
Could not resolve com.github.groom:c4ang-contract-hub:develop-SNAPSHOT
```

**원인**: JitPack 빌드 실패 또는 첫 요청

**해결책**:
1. JitPack 빌드 로그 확인:
   ```
   https://jitpack.io/com/github/groom/c4ang-contract-hub/develop-SNAPSHOT/build.log
   ```

2. JitPack에서 수동 빌드 트리거:
   ```
   https://jitpack.io/#groom/c4ang-contract-hub
   ```
   "Get It" 버튼 클릭

3. 빌드 성공 확인 후 Gradle 캐시 갱신:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

### 문제 2: SNAPSHOT 버전이 업데이트 안됨

**증상**: develop 브랜치가 업데이트됐는데 Consumer에 반영 안됨

**원인**: Gradle 캐시가 SNAPSHOT 버전을 캐싱

**해결책**:
```bash
# 1. Gradle 캐시 삭제
./gradlew clean

# 2. 의존성 강제 갱신
./gradlew build --refresh-dependencies

# 3. JitPack 캐시 확인
# https://jitpack.io/#groom/c4ang-contract-hub/develop-SNAPSHOT
```

### 문제 3: Feature 브랜치 버전을 찾을 수 없음

**증상**:
```
Could not find com.github.groom:c4ang-contract-hub:feature-user-auth-SNAPSHOT
```

**원인**:
- 브랜치명에 슬래시(`/`) 포함 → JitPack이 `-`로 변환
- 아직 JitPack이 해당 브랜치를 빌드하지 않음

**해결책**:
```kotlin
// feature/user-auth 브랜치의 경우
implementation("com.github.groom:c4ang-contract-hub:feature-user-auth-SNAPSHOT")
// 슬래시(/)는 하이픈(-)으로 자동 변환됨
```

JitPack에서 수동 빌드:
```
https://jitpack.io/#groom/c4ang-contract-hub
```

### 문제 4: Schema Registry 호환성 오류

**증상**:
```
Schema being registered is incompatible with an earlier schema
```

**원인**: Avro 스키마 하위 호환성 문제

**해결책**:
1. Contract Hub에서 스키마 하위 호환 가능하게 수정:
   ```json
   {
     "name": "newField",
     "type": "string",
     "default": "",  // default 값 필수!
     "doc": "새 필드"
   }
   ```

2. 또는 Schema Registry 호환성 체크 임시 비활성화:
   ```bash
   curl -X PUT http://localhost:8081/config \
     -H "Content-Type: application/json" \
     -d '{"compatibility": "NONE"}'
   ```

---

## 📊 버전 전략 비교

### 이 프로젝트의 선택: JitPack Branch 기반

| 장점 | 단점 |
|------|------|
| ✅ 버전 관리 단순 (build.gradle.kts 수정 불필요) | ⚠️ 공개 저장소만 무료 |
| ✅ JitPack 무료 사용 | ⚠️ 첫 빌드 시 시간 소요 (1-3분) |
| ✅ 브랜치명이 곧 버전 (직관적) | ⚠️ Semantic Versioning 미준수 (develop에 버전 번호 없음) |
| ✅ CI/CD 설정 최소화 | ⚠️ SNAPSHOT 캐싱 이슈 가능 |

### 대안: Semantic Versioning + Suffix

만약 나중에 더 엄격한 버전 관리가 필요하면, 다음과 같이 변경 가능:

```
main:       1.0.0, 1.1.0
develop:    1.1.0-SNAPSHOT (버전 번호 포함)
feature:    1.1.0-feature-xxx-SNAPSHOT
```

이 경우 build.gradle.kts에서 버전을 동적으로 계산해야 함:
```kotlin
version = when (val branch = System.getenv("GITHUB_REF_NAME") ?: "main") {
    "main" -> project.findProperty("releaseVersion") as String? ?: "1.0.0"
    "develop" -> "${project.findProperty("nextVersion") as String? ?: "1.1.0"}-SNAPSHOT"
    else -> "${project.findProperty("nextVersion") as String? ?: "1.1.0"}-${branch.replace("/", "-")}-SNAPSHOT"
}
```

---

## 📚 참고 자료

- [JitPack 공식 문서](https://jitpack.io/docs/)
- [Semantic Versioning](https://semver.org/)
- [Git Flow 전략](https://nvie.com/posts/a-successful-git-branching-model/)
- [Gradle 의존성 관리](https://docs.gradle.org/current/userguide/dependency_management.html)

---

## 요약

### Contract Hub (이 프로젝트)
```bash
# Feature 개발
git checkout -b feature/user-auth
git push origin feature/user-auth
# → JitPack: feature-user-auth-SNAPSHOT

# Develop 통합
git checkout develop
git merge feature/user-auth
git push origin develop
# → JitPack: develop-SNAPSHOT

# Production 릴리스
git checkout main
git merge develop
git tag v1.0.0
git push origin v1.0.0
# → JitPack: 1.0.0
```

### Consumer/Producer 프로젝트
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // 환경별 버전 선택
    implementation("com.github.groom:c4ang-contract-hub:1.0.0")  // Production
    // implementation("com.github.groom:c4ang-contract-hub:develop-SNAPSHOT")  // Development
}
```

```bash
# SNAPSHOT 업데이트 시
./gradlew build --refresh-dependencies
```
