# Gradle buildSrc 적용

## buildSrc란?

`buildSrc`는 **Gradle의 특별한 디렉토리**로, 빌드 로직을 커스터마이징하는 프로젝트 내부 모듈입니다.

### 핵심 특징

#### 1. 자동으로 인식되는 특별한 이름
- Gradle이 빌드 시 자동으로 `buildSrc/`를 찾아서 먼저 빌드
- 별도 설정 없이 사용 가능
- 프로젝트 루트에 `buildSrc/` 디렉토리만 생성하면 됨

#### 2. 빌드 스크립트에서 사용 가능
- buildSrc에 작성한 클래스/함수를 `build.gradle.kts`에서 직접 사용
- import 문구 없이 바로 접근 가능
- 타입 이름만으로 참조

#### 3. 타입 안전한 빌드 로직
- Kotlin/Java로 빌드 로직 작성
- IDE 자동완성, 리팩토링 지원
- 컴파일 타임 타입 체크
- 유닛 테스트 작성 가능

---

## 프로젝트 구조

### 현재 프로젝트의 buildSrc 구조

```
c4ang-contract-hub/
├── build.gradle.kts              # 메인 빌드 스크립트
├── settings.gradle.kts
└── buildSrc/                     # buildSrc 모듈
    ├── build.gradle.kts          # buildSrc 자체의 빌드 설정
    └── src/
        └── main/
            └── kotlin/
                └── AvroDocGenerator.kt  # 커스텀 Gradle Task
```

### buildSrc/build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
}
```

**설명**:
- `kotlin-dsl` 플러그인: Kotlin으로 빌드 로직 작성
- buildSrc 자체의 의존성 정의
- AvroDocGenerator가 사용할 라이브러리 추가

---

## 이 프로젝트에서의 사용: AvroDocGenerator

### 목적
Avro 스키마 파일(.avsc)로부터 이벤트 명세 마크다운 문서를 자동 생성하는 커스텀 Gradle Task

### 구현

```kotlin
// buildSrc/src/main/kotlin/AvroDocGenerator.kt
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Avro 스키마 파일(.avsc)로부터 이벤트 명세 마크다운 문서를 자동 생성하는 Gradle Task
 *
 * 사용법:
 * ./gradlew generateAvroEventDocs
 *
 * 기능:
 * - Avro 스키마 파일을 읽어서 이벤트 명세를 자동 생성
 * - 이벤트 흐름 문서의 이벤트 명세 섹션을 자동 업데이트
 * - 단일 진실 공급원(Single Source of Truth): Avro 스키마
 */
open class AvroDocGenerator : DefaultTask() {

    @InputDirectory
    val avroSchemaDir = project.file("src/main/avro/events")

    @OutputDirectory
    val outputDir = project.file("docs/generated")

    @TaskAction
    fun generate() {
        // 1. Avro 스키마 파일 읽기
        // 2. 마크다운 문서 생성
        // 3. event-flows 문서 자동 업데이트
    }
}
```

### build.gradle.kts에서 사용

```kotlin
// build.gradle.kts (메인 빌드 스크립트)

// AvroDocGenerator를 Task로 등록
tasks.register<AvroDocGenerator>("generateAvroEventDocs") {
    group = "documentation"
    description = "Avro 스키마로부터 이벤트 명세 문서를 자동 생성합니다"
}

// 빌드 시 자동 실행
tasks.named("build") {
    dependsOn("generateAvroEventDocs")
}
```

**주목할 점**:
- `AvroDocGenerator` 타입을 **import 없이 바로 사용**
- IDE에서 자동완성 지원
- 컴파일 타임에 타입 체크

---

## buildSrc 사용 이유

### 일반적인 방법 vs buildSrc

#### 방법 1: build.gradle.kts에 직접 작성 (비추천)

```kotlin
// build.gradle.kts
tasks.register("generateAvroEventDocs") {
    doLast {
        val avroDir = file("src/main/avro/events")
        val outputDir = file("docs/generated")

        // 200줄 이상의 복잡한 로직...
        // - 타입 체크 없음
        // - IDE 지원 제한적
        // - 재사용 어려움
        // - 테스트 불가능
        // - 빌드 스크립트가 지저분해짐

        avroDir.listFiles()?.forEach { file ->
            // JSON 파싱
            // 마크다운 생성
            // 파일 쓰기
            // ...
        }
    }
}
```

**문제점**:
- ❌ 타입 안정성 부족
- ❌ IDE 자동완성 제한적
- ❌ 유닛 테스트 불가능
- ❌ 코드 재사용 어려움
- ❌ build.gradle.kts가 너무 길어짐

#### 방법 2: buildSrc 사용 (현재 방식) ⭐

```kotlin
// buildSrc/src/main/kotlin/AvroDocGenerator.kt
open class AvroDocGenerator : DefaultTask() {
    @InputDirectory
    val avroSchemaDir = project.file("src/main/avro/events")

    @OutputDirectory
    val outputDir = project.file("docs/generated")

    @TaskAction
    fun generate() {
        // 타입 안전한 로직
        // IDE 지원 완벽
        // 테스트 가능
        // 재사용 쉬움
    }

    private fun generateEventMarkdown(schema: Map<String, Any>): String {
        // 헬퍼 메서드로 분리 가능
    }
}

// build.gradle.kts (간결!)
tasks.register<AvroDocGenerator>("generateAvroEventDocs") {
    group = "documentation"
    description = "문서 자동 생성"
}
```

**장점**:
- ✅ 타입 안정성 (컴파일 타임 체크)
- ✅ IDE 완벽 지원 (자동완성, 리팩토링)
- ✅ 유닛 테스트 가능
- ✅ 코드 재사용 쉬움
- ✅ build.gradle.kts가 깔끔해짐
- ✅ 비즈니스 로직과 분리

---

## buildSrc vs Gradle Plugin

### buildSrc (현재 방식)

**용도**: 이 프로젝트 전용 빌드 로직

**범위**: 현재 프로젝트에서만 사용

**설정**:
```
c4ang-contract-hub/
└── buildSrc/              ← 디렉토리만 생성하면 끝
    ├── build.gradle.kts
    └── src/main/kotlin/
```

**장점**:
- ✅ 설정이 매우 간단
- ✅ 프로젝트와 함께 버전 관리
- ✅ 별도 배포 불필요

**단점**:
- ❌ 다른 프로젝트에서 재사용 불가
- ❌ 빌드 캐시 비활성화 (buildSrc 변경 시 전체 재빌드)

### Gradle Plugin (별도 배포)

**용도**: 여러 프로젝트에서 재사용

**범위**: 다른 프로젝트에서도 사용 가능

**설정**:
```
avro-doc-plugin/          ← 별도 프로젝트
├── build.gradle.kts
├── src/main/kotlin/
│   └── AvroDocPlugin.kt
└── publish 설정...

c4ang-contract-hub/
└── build.gradle.kts
    plugins {
        id("com.c4ang.avro-doc") version "1.0"  ← 플러그인 사용
    }
```

**장점**:
- ✅ 여러 프로젝트에서 재사용
- ✅ 버전 관리 독립적
- ✅ 빌드 캐시 활용 가능

**단점**:
- ❌ 설정이 복잡 (별도 프로젝트, 배포 설정)
- ❌ Maven/Gradle Repository에 배포 필요
- ❌ 버전 업데이트 관리 필요

### 이 프로젝트는 buildSrc가 적합한 이유

1. **AvroDocGenerator는 이 프로젝트 전용**
   - 다른 프로젝트에서 재사용할 필요 없음
   - c4ang-contract-hub의 디렉토리 구조에 특화

2. **설정이 간단**
   - 디렉토리만 만들면 자동 인식
   - 별도 배포 설정 불필요

3. **프로젝트와 함께 진화**
   - 프로젝트 요구사항 변경 시 즉시 수정 가능
   - 버전 관리가 프로젝트와 동기화

---

## buildSrc 빌드 순서

Gradle은 다음 순서로 빌드를 수행합니다:

```
1. buildSrc/ 빌드
   ├─> buildSrc/build.gradle.kts 평가
   ├─> buildSrc/src/main/kotlin/ 컴파일
   ├─> AvroDocGenerator.class 생성
   └─> buildSrc.jar 생성

2. build.gradle.kts 평가
   ├─> AvroDocGenerator 타입 인식 가능
   ├─> tasks.register<AvroDocGenerator>(...) 성공
   └─> Task 그래프 생성

3. 메인 프로젝트 빌드
   ├─> 요청된 Task 실행
   └─> generateAvroEventDocs Task 실행 (필요시)
```

### 실행 예시

```bash
$ ./gradlew generateAvroEventDocs

> Configure project :
> Task :buildSrc:compileKotlin           ← buildSrc 먼저 빌드
> Task :buildSrc:compileJava
> Task :buildSrc:jar
> Task :generateAvroEventDocs            ← 커스텀 Task 실행
✅ Avro 이벤트 문서 생성 완료: docs/generated/event-specifications.md
   - 업데이트: event-flows/order-saga/README.md
   생성된 이벤트 수: 7

BUILD SUCCESSFUL in 3s
4 actionable tasks: 4 executed
```

---

## buildSrc 장단점 비교

### 장점 정리

| 특징 | build.gradle.kts 직접 | buildSrc | Gradle Plugin |
|------|----------------------|----------|---------------|
| **타입 안정성** | ❌ 동적 스크립트 | ✅ 컴파일 타임 체크 | ✅ 컴파일 타임 체크 |
| **IDE 지원** | ⚠️ 제한적 | ✅ 완벽 | ✅ 완벽 |
| **재사용성** | ❌ 어려움 | ⚠️ 프로젝트 내부만 | ✅ 모든 프로젝트 |
| **테스트** | ❌ 불가능 | ✅ 가능 | ✅ 가능 |
| **리팩토링** | ❌ 위험 | ✅ 안전 | ✅ 안전 |
| **설정 복잡도** | ✅ 매우 간단 | ✅ 간단 | ❌ 복잡 |
| **배포 필요** | ✅ 불필요 | ✅ 불필요 | ❌ 필요 |
| **빌드 캐시** | ✅ 가능 | ⚠️ 제한적 | ✅ 가능 |
| **버전 관리** | ⚠️ 스크립트와 섞임 | ✅ 프로젝트와 동기화 | ⚠️ 별도 관리 |

### 단점 및 주의사항

#### buildSrc 변경 시 전체 재빌드

```bash
# buildSrc/src/main/kotlin/AvroDocGenerator.kt 수정

$ ./gradlew build
> Task :buildSrc:compileKotlin
> Task :buildSrc:jar
> Task :compileKotlin               ← 메인 프로젝트도 재컴파일
> Task :compileTestKotlin           ← 테스트도 재컴파일
...
```

**이유**: buildSrc가 변경되면 빌드 스크립트가 의존하는 클래스가 변경된 것이므로 전체 재평가 필요

**완화 방법**:
- buildSrc를 자주 수정하지 않도록 안정화
- 개발 중에는 로컬에서 테스트
- CI/CD에서는 캐시 활용

---

## buildSrc 활용 가이드

### 1. buildSrc 생성

```bash
# 프로젝트 루트에서
mkdir -p buildSrc/src/main/kotlin
```

### 2. buildSrc/build.gradle.kts 작성

```kotlin
plugins {
    `kotlin-dsl`  // Kotlin으로 빌드 로직 작성
}

repositories {
    mavenCentral()
}

dependencies {
    // 커스텀 Task에서 사용할 라이브러리
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
}
```

### 3. 커스텀 Task 작성

```kotlin
// buildSrc/src/main/kotlin/MyCustomTask.kt
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class MyCustomTask : DefaultTask() {

    @TaskAction
    fun execute() {
        println("커스텀 Task 실행!")
        // 비즈니스 로직...
    }
}
```

### 4. build.gradle.kts에서 사용

```kotlin
// build.gradle.kts
tasks.register<MyCustomTask>("myTask") {
    group = "custom"
    description = "나의 커스텀 Task"
}
```

### 5. Task 실행

```bash
./gradlew myTask
```

---

## 실전 예제: AvroDocGenerator

### 요구사항

- Avro 스키마 파일(.avsc)을 읽어서 마크다운 문서 생성
- 이벤트 플로우 문서에 자동으로 명세 삽입
- 단일 진실 공급원(Single Source of Truth) 구현

### buildSrc를 선택한 이유

1. **프로젝트 전용 로직**
   - c4ang-contract-hub의 디렉토리 구조에 특화
   - `src/main/avro/events/`, `event-flows/` 경로에 의존
   - 다른 프로젝트에서 재사용 불가능

2. **복잡한 로직 분리**
   - 200줄 이상의 코드를 build.gradle.kts에 작성하면 가독성 저하
   - 타입 안전성 필요 (JSON 파싱, 파일 I/O)
   - 여러 헬퍼 메서드로 분리 필요

3. **유지보수성**
   - Avro 스키마 구조 변경 시 수정 필요
   - 테스트 코드 작성 가능
   - IDE 지원으로 안전한 리팩토링

### 구현 핵심

```kotlin
open class AvroDocGenerator : DefaultTask() {

    @InputDirectory
    val avroSchemaDir = project.file("src/main/avro/events")

    @OutputDirectory
    val outputDir = project.file("docs/generated")

    @TaskAction
    fun generate() {
        val mapper = ObjectMapper().registerKotlinModule()
        val avroFiles = avroSchemaDir.listFiles { file ->
            file.extension == "avsc"
        } ?: emptyArray()

        // 1. 전체 이벤트 명세 문서 생성
        generateEventSpecifications(avroFiles, mapper)

        // 2. 이벤트 플로우 문서 자동 업데이트
        updateEventFlowDocuments(avroFiles, mapper)
    }

    private fun generateEventSpecifications(files: Array<File>, mapper: ObjectMapper) {
        // 마크다운 생성 로직
    }

    private fun updateEventFlowDocuments(files: Array<File>, mapper: ObjectMapper) {
        // AUTO_GENERATED 섹션 업데이트 로직
    }
}
```

---

## buildSrc 테스트

### 테스트 디렉토리 구조

```
buildSrc/
├── build.gradle.kts
└── src/
    ├── main/kotlin/
    │   └── AvroDocGenerator.kt
    └── test/kotlin/                    ← 테스트 추가
        └── AvroDocGeneratorTest.kt
```

### 테스트 코드 예시

```kotlin
// buildSrc/src/test/kotlin/AvroDocGeneratorTest.kt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class AvroDocGeneratorTest {

    @Test
    fun `Kafka 토픽 추론 테스트`() {
        val generator = AvroDocGenerator()

        val topic = generator.inferKafkaTopic("OrderCreatedEvent")

        assertEquals("c4ang.order.created", topic)
    }

    @Test
    fun `마크다운 생성 테스트`(@TempDir tempDir: File) {
        // Given
        val avroSchema = """
            {
              "type": "record",
              "name": "TestEvent",
              "fields": [
                {"name": "id", "type": "string"}
              ]
            }
        """.trimIndent()

        // When
        val markdown = generateMarkdown(avroSchema)

        // Then
        assertTrue(markdown.contains("TestEvent"))
        assertTrue(markdown.contains("| id | string |"))
    }
}
```

### buildSrc/build.gradle.kts에 테스트 의존성 추가

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")

    // 테스트 의존성
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### 테스트 실행

```bash
# buildSrc 테스트 실행
./gradlew :buildSrc:test

# 전체 테스트 (buildSrc + 메인 프로젝트)
./gradlew test
```

---

## buildSrc 베스트 프랙티스

### 1. 적절한 사용 시점

**buildSrc를 사용해야 할 때**:
- ✅ 복잡한 빌드 로직 (50줄 이상)
- ✅ 타입 안전성이 필요한 경우
- ✅ 여러 곳에서 재사용하는 로직
- ✅ 테스트가 필요한 로직
- ✅ build.gradle.kts가 지저분해질 때

**buildSrc를 사용하지 말아야 할 때**:
- ❌ 간단한 Task (5줄 미만)
- ❌ 일회성 스크립트
- ❌ 여러 프로젝트에서 공유해야 하는 경우 (→ Gradle Plugin)

### 2. 파일 구조 조직화

```
buildSrc/
└── src/main/kotlin/
    ├── tasks/                  # Task 클래스
    │   ├── AvroDocGenerator.kt
    │   └── ContractValidator.kt
    ├── extensions/             # Extension 함수/클래스
    │   └── ProjectExtensions.kt
    └── utils/                  # 유틸리티
        └── FileUtils.kt
```

### 3. 명확한 Task 이름

```kotlin
// Good ✅
tasks.register<AvroDocGenerator>("generateAvroEventDocs")

// Bad ❌
tasks.register<AvroDocGenerator>("gen")
tasks.register<AvroDocGenerator>("avro")
```

### 4. Task Group 지정

```kotlin
tasks.register<AvroDocGenerator>("generateAvroEventDocs") {
    group = "documentation"              // Task를 그룹화
    description = "Avro 스키마로부터 이벤트 명세 문서를 자동 생성합니다"
}
```

```bash
# Group별로 Task 확인
./gradlew tasks --group=documentation

Documentation tasks
-------------------
generateAvroEventDocs - Avro 스키마로부터 이벤트 명세 문서를 자동 생성합니다
```

### 5. Input/Output 명시

```kotlin
open class AvroDocGenerator : DefaultTask() {

    @InputDirectory                     // 입력 디렉토리 명시
    val avroSchemaDir = project.file("src/main/avro/events")

    @OutputDirectory                    // 출력 디렉토리 명시
    val outputDir = project.file("docs/generated")

    @TaskAction
    fun generate() {
        // ...
    }
}
```

**장점**:
- Gradle이 증분 빌드(Incremental Build) 지원
- Input이 변경되지 않으면 Task 스킵 (UP-TO-DATE)
- 빌드 성능 향상

---

## 트러블슈팅

### 문제 1: buildSrc 변경 후 Task를 찾을 수 없음

**증상**:
```bash
$ ./gradlew generateAvroEventDocs

FAILURE: Build failed with an exception.
* What went wrong:
Task 'generateAvroEventDocs' not found in root project
```

**원인**: buildSrc 빌드 실패

**해결책**:
```bash
# buildSrc만 빌드해서 오류 확인
./gradlew :buildSrc:build

# 오류 수정 후 다시 시도
./gradlew generateAvroEventDocs
```

### 문제 2: buildSrc 클래스를 인식하지 못함

**증상**: IDE에서 `AvroDocGenerator`를 찾을 수 없다는 오류

**해결책**:
1. Gradle 프로젝트 재로드 (`Cmd/Ctrl + Shift + I`)
2. IDE 캐시 무효화 (`File` → `Invalidate Caches...`)
3. buildSrc 빌드 확인: `./gradlew :buildSrc:build`

### 문제 3: buildSrc 의존성 충돌

**증상**:
```
Caused by: java.lang.NoClassDefFoundError: com/fasterxml/jackson/databind/ObjectMapper
```

**원인**: buildSrc의 의존성과 메인 프로젝트의 의존성 버전 충돌

**해결책**:
```kotlin
// buildSrc/build.gradle.kts
dependencies {
    // 메인 프로젝트와 동일한 버전 사용
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
}
```

---

## 참고 자료

- [Gradle 공식 문서 - Organizing Build Logic](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources)
- [Gradle buildSrc vs Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html)
- [Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

---

## 요약

### buildSrc는?
- Gradle의 **특별한 디렉토리**
- 커스텀 빌드 로직을 Kotlin/Java로 작성하는 공간
- **타입 안전**하고 **재사용 가능**한 빌드 Task 정의

### 이 프로젝트에서는?
- `AvroDocGenerator` 커스텤 Task를 정의
- Avro 스키마 → 마크다운 문서 자동 생성
- `./gradlew generateAvroEventDocs`로 실행

### 왜 buildSrc를 사용했나?
1. **복잡한 로직 분리** (200줄 이상 → build.gradle.kts가 지저분)
2. **타입 안전성** (JSON 파싱, 파일 I/O)
3. **재사용성** (여러 헬퍼 메서드로 분리)
4. **테스트 가능** (유닛 테스트 작성)
5. **프로젝트 전용** (별도 플러그인까지는 불필요)

### buildSrc vs 대안

| | build.gradle.kts | buildSrc | Gradle Plugin |
|---|---|---|---|
| **타입 안정성** | ❌ | ✅ | ✅ |
| **IDE 지원** | ⚠️ | ✅ | ✅ |
| **설정 복잡도** | ✅ | ✅ | ❌ |
| **재사용 범위** | - | 프로젝트 내부 | 모든 프로젝트 |
| **적합한 경우** | 간단한 로직 | 프로젝트 전용 로직 | 공유 로직 |

→ **이 프로젝트는 buildSrc가 가장 적합!** 🎯
