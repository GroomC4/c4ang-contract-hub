# Contract Test 가이드

## 개요

Consumer Driven Contract(CDC) 테스트는 **Producer**와 **Consumer** 양쪽에서 수행됩니다.
각각의 역할과 필요한 환경이 다르므로, 프로필 전략도 달라야 합니다.

---

## Producer vs Consumer Contract Test

### 테스트 방식 비교

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Contract Test 아키텍처                                │
├─────────────────────────────────┬───────────────────────────────────────────┤
│     Producer Contract Test      │       Consumer Contract Test              │
│     (Product Service 등)        │       (Order Service 등)                  │
├─────────────────────────────────┼───────────────────────────────────────────┤
│                                 │                                           │
│  ┌─────────────────────┐        │  ┌─────────────────────┐                  │
│  │   Contract (YAML)   │        │  │   Contract (YAML)   │                  │
│  └──────────┬──────────┘        │  └──────────┬──────────┘                  │
│             │                   │             │                             │
│             ▼                   │             ▼                             │
│  ┌─────────────────────┐        │  ┌─────────────────────┐                  │
│  │ 자동 생성된 테스트  │        │  │    Stub JAR 생성    │                  │
│  │ (Spring Cloud       │        │  │ (Producer가 배포)   │                  │
│  │  Contract Verifier) │        │  └──────────┬──────────┘                  │
│  └──────────┬──────────┘        │             │                             │
│             │                   │             ▼                             │
│             ▼                   │  ┌─────────────────────┐                  │
│  ┌─────────────────────┐        │  │   Stub Runner       │                  │
│  │      MockMvc        │        │  │   (WireMock 시작)   │                  │
│  │  (실제 API 호출)    │        │  └──────────┬──────────┘                  │
│  └──────────┬──────────┘        │             │                             │
│             │                   │             ▼                             │
│             ▼                   │  ┌─────────────────────┐                  │
│  ┌─────────────────────┐        │  │   FeignClient       │                  │
│  │   실제 Controller   │        │  │  (WireMock 호출)    │                  │
│  │   실제 Service      │        │  └──────────┬──────────┘                  │
│  │   실제 Repository   │        │             │                             │
│  └──────────┬──────────┘        │             ▼                             │
│             │                   │  ┌─────────────────────┐                  │
│             ▼                   │  │  Contract 준수 검증 │                  │
│  ┌─────────────────────┐        │  └─────────────────────┘                  │
│  │   실제 Database     │        │                                           │
│  │   (Testcontainers)  │        │       인프라 불필요                       │
│  └─────────────────────┘        │                                           │
│                                 │                                           │
│    ✅ DB, Redis, Kafka 필요    │    ✅ 인프라 없이 빠르게 실행              │
│    ✅ test 프로필 사용         │    ✅ consumer-contract-test 프로필        │
│                                 │                                           │
└─────────────────────────────────┴───────────────────────────────────────────┘
```

### 환경이 다른 이유

| 구분 | Producer Contract Test | Consumer Contract Test |
|------|------------------------|------------------------|
| **목적** | 내 API가 Contract를 만족하는지 검증 | 내가 호출하는 API가 Contract대로 동작하는지 검증 |
| **검증 대상** | 실제 Controller, Service, Repository | FeignClient → WireMock (Stub) |
| **DB 필요** | **필요** (실제 데이터 조회/저장) | 불필요 |
| **Redis 필요** | **필요** (캐시/재고 관련 API) | 불필요 |
| **Kafka 필요** | 애플리케이션 컨텍스트 로드에 필요 | 불필요 |
| **프로필** | `test` | `consumer-contract-test` |
| **실행 시간** | 느림 (30초+) | 빠름 (5초) |

### 프로필 전략

```
┌────────────────────────────────────────────────────────────────┐
│                       프로필 전략                               │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Producer (Product Service, Store Service 등)                  │
│  ─────────────────────────────────────────────                 │
│  프로필: test                                                  │
│  이유: MockMvc로 실제 API를 호출하므로 DB, Redis 등 필요       │
│                                                                │
│  Consumer (Order Service 등)                                   │
│  ─────────────────────────────────                             │
│  프로필: consumer-contract-test                                │
│  이유: WireMock만 호출하므로 인프라 불필요, TestClient 비활성화│
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Producer Contract Test (API 제공자)

### 개요

Producer는 Contract 파일(YAML)을 기반으로 자동 생성된 테스트를 실행합니다.
**실제 API를 MockMvc로 호출**하므로 DB, Redis 등 인프라가 필요합니다.

### 프로필: `test`

Producer Contract Test는 `test` 프로필을 사용합니다.

```kotlin
// ContractTestBase.kt (Producer)
@IntegrationTest  // Testcontainers 활성화
@SpringBootTest(
    properties = [
        "spring.profiles.active=test",
        "testcontainers.postgres.enabled=true",
        "testcontainers.redis.enabled=true",
        "testcontainers.kafka.enabled=true",
        // ... Kafka topics 설정
    ],
)
@AutoConfigureMockMvc
@SqlGroup(
    Sql(scripts = ["/sql/contract-test-data.sql"], executionPhase = BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/cleanup.sql"], executionPhase = AFTER_TEST_METHOD),
)
abstract class ContractTestBase {
    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @BeforeEach
    fun setup() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext)
    }
}
```

### 실행 방법

```bash
# Producer에서 Contract Test 실행
./gradlew contractTest
```

### Stub JAR 배포

```bash
# Consumer가 사용할 Stub JAR 배포
./gradlew publishStubsPublicationToMavenLocal  # 로컬
./gradlew publish                              # 원격 저장소
```

---

## Consumer Contract Test (API 사용자)

### 개요

Consumer는 Producer가 배포한 Stub JAR을 사용하여 FeignClient가 Contract대로 동작하는지 검증합니다.
**WireMock만 호출**하므로 인프라가 불필요합니다.

### 프로필이 분리되어야 하는 이유

#### 문제 1: 불필요한 인프라 초기화

`test` 프로필 사용 시 Testcontainers가 PostgreSQL, Redis를 시작합니다.

```
Consumer Contract Test 실행 (test 프로필)
    → Testcontainers 시작 (PostgreSQL, Redis)
    → 30초+ 소요
    → 실제로 DB를 사용하지 않음
    → 리소스 낭비
```

#### 문제 2: TestClient와 FeignClient 충돌

`test` 프로필에서는 `TestProductClient`가 `@Primary`로 등록되어 실제 FeignClient를 대체합니다.

```kotlin
// test 프로필에서 활성화
@Component
@Profile("test")
@Primary
class TestProductClient : ProductClient {
    // Stub 데이터 반환 (WireMock 호출 안 함)
}
```

Contract Test는 **실제 FeignClient가 WireMock을 호출**해야 하므로 충돌이 발생합니다.

```
test 프로필                          consumer-contract-test 프로필
─────────────                        ─────────────────────────────
ProductClient                        ProductClient
      ↓                                    ↓
TestProductClient (@Primary)         ProductFeignClient
      ↓                                    ↓
Stub 데이터 반환                      WireMock 서버 호출
      ↓                                    ↓
❌ Contract 검증 불가                 ✅ Contract 검증 가능
```

### 프로필: `consumer-contract-test`

Consumer Contract Test는 `consumer-contract-test` 프로필을 사용합니다.

#### application-consumer-contract-test.yml

```yaml
# ====================================
# Consumer Contract Test 프로필
# ====================================
# Stub Runner가 WireMock 서버를 띄우고, FeignClient가 해당 서버로 요청합니다.

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 5000
  # JPA/DataSource 비활성화 (Consumer Contract Test는 DB 불필요)
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.redisson.spring.starter.RedissonAutoConfigurationV2

# Feign Client 설정 (Stub Runner가 제공하는 WireMock 서버 포트 사용)
feign:
  clients:
    product-service:
      url: http://localhost:8083
    store-service:
      url: http://localhost:8084

# Stub Runner 설정
stubrunner:
  ids-to-service-ids:
    product-api: product-service
    store-api: store-service

logging:
  level:
    org.springframework.cloud.contract: DEBUG
    com.groom.order.adapter.outbound.client: DEBUG
```

#### 주요 설정 설명

| 설정 | 목적 |
|------|------|
| `spring.autoconfigure.exclude` | DB, Redis, JPA 자동 설정 비활성화 |
| `feign.clients.*.url` | WireMock 서버 주소 (Stub Runner가 해당 포트에 서버 시작) |
| `stubrunner.ids-to-service-ids` | Stub artifact ID와 Feign Client name 매핑 |

### 테스트 코드 구조

```kotlin
@Tag("consumer-contract-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("consumer-contract-test")  // consumer-contract-test 프로필 사용
@AutoConfigureStubRunner(
    ids = ["io.github.groomc4:product-api:+:stubs:8083"],
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
)
class ProductClientContractTest {

    @Autowired
    private lateinit var productFeignClient: ProductFeignClient  // 실제 FeignClient 주입

    @Test
    fun `상품 단건 조회 - 성공`() {
        val response = productFeignClient.getProduct(EXISTING_PRODUCT_ID)

        response.shouldNotBeNull()
        response.id shouldBe EXISTING_PRODUCT_ID
    }
}
```

### @AutoConfigureStubRunner 설정

```kotlin
@AutoConfigureStubRunner(
    ids = ["io.github.groomc4:product-api:+:stubs:8083"],
    //     ─────────────────────────────────────────────
    //     groupId:artifactId:version:classifier:port
    //
    //     +: 최신 버전
    //     stubs: stub classifier
    //     8083: WireMock 서버 포트

    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    // LOCAL: ~/.m2/repository에서 stub 조회
    // REMOTE: repositoryRoot에서 stub 다운로드
)
```

### build.gradle.kts 설정

```kotlin
// Consumer Contract Test 전용 태스크 (Stub Runner 기반)
val consumerContractTest by tasks.registering(Test::class) {
    description = "Runs consumer contract tests with Stub Runner"
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("consumer-contract-test")  // @Tag("consumer-contract-test") 테스트만 실행
    }
}
```

### 실행 방법

```bash
# Consumer에서 Contract Test 실행
./gradlew consumerContractTest
```

---

## 프로필별 비교 (전체)

| 항목 | test | consumer-contract-test |
|------|------|------------------------|
| **용도** | 단위/통합 테스트 + Producer Contract Test | Consumer Contract Test |
| **DB 초기화** | O (Testcontainers) | X |
| **Redis 초기화** | O (Testcontainers) | X |
| **Kafka 초기화** | O (Testcontainers) | X |
| **Client** | TestClient (stub) | FeignClient (WireMock) |
| **실행 시간** | 느림 (30초+) | 빠름 (5초) |
| **Gradle 태스크** | `test`, `contractTest` | `consumerContractTest` |
| **JUnit 태그** | - | `consumer-contract-test` |

---

## 사전 조건

### Producer가 Stub JAR 배포 필요

```bash
# Product Service에서 실행
./gradlew publishStubsPublicationToMavenLocal

# 또는 원격 저장소에 배포
./gradlew publish
```

### Stub JAR 위치 확인

```bash
# 로컬 Maven 저장소
ls ~/.m2/repository/io/github/groomc4/product-api/*/product-api-*-stubs.jar
```

---

## 트러블슈팅

### Stub JAR를 찾을 수 없음

```
No stub found for io.github.groomc4:product-api:+:stubs
```

**해결**: Producer에서 Stub JAR 배포 확인

```bash
ls ~/.m2/repository/io/github/groomc4/product-api/
```

### WireMock 포트 충돌

```
Port 8083 is already in use
```

**해결**: 다른 포트 사용 또는 기존 프로세스 종료

```kotlin
@AutoConfigureStubRunner(
    ids = ["io.github.groomc4:product-api:+:stubs:18083"],  // 포트 변경
)
```

### TestClient가 주입됨 (Consumer)

```
Expected WireMock call but got stub data
```

**해결**: `@ActiveProfiles("consumer-contract-test")` 확인 (test가 아닌 consumer-contract-test)

### DB 연결 오류 (Producer)

```
Failed to determine a suitable driver class
```

**해결**: Producer Contract Test는 `test` 프로필 사용 확인, Testcontainers 설정 확인

---

## 파일 위치

### Producer (Product Service)

```
product-api/
├── build.gradle.kts                          # contractTest 태스크 (Spring Cloud Contract 제공)
└── src/test/
    ├── kotlin/.../common/
    │   └── ContractTestBase.kt               # Producer Contract Test Base
    └── resources/
        ├── application-test.yml              # test 프로필 (Testcontainers)
        ├── contracts.internal/               # Contract 파일 (YAML)
        │   └── order-service/
        │       └── getProductById_success.yml
        └── sql/
            └── contract-test-data.sql        # 테스트 데이터
```

### Consumer (Order Service)

```
order-api/
├── build.gradle.kts                          # consumerContractTest 태스크, stub-runner 의존성
└── src/test/
    ├── kotlin/.../contract/
    │   ├── ProductClientContractTest.kt      # Product 계약 테스트
    │   └── StoreClientContractTest.kt        # Store 계약 테스트
    └── resources/
        ├── application-test.yml              # 일반 테스트 설정
        └── application-consumer-contract-test.yml  # Consumer Contract 테스트 설정
```

---

## 요약

| 역할 | 프로필 | 인프라 | Gradle 태스크 |
|------|--------|--------|---------------|
| **Producer** | `test` | 필요 (DB, Redis, Kafka) | `./gradlew contractTest` |
| **Consumer** | `consumer-contract-test` | 불필요 | `./gradlew consumerContractTest` |
