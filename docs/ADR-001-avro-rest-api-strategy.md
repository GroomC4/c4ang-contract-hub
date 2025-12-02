# ADR-001: Avro 기반 REST API 통신 전략

## 상태
**제안됨 (Proposed)** - 향후 고도화 검토 대상

## 배경

### 현재 상황
- `c4ang-contract-hub`는 동기/비동기 통신 스키마를 통합 관리하는 라이브러리
- 디렉토리 구조:
  - `src/main/events/avro/` - Kafka 비동기 이벤트 스키마 (23개)
  - `src/main/api/avro/` - REST API 동기 통신 스키마 (4개)

### 문제 발생 (customer-service)
`c4ang-customer-service`에서 contract-hub의 Avro 클래스를 REST API 응답으로 직접 사용하려 했으나 실패:

```
HttpMessageNotWritableException: Avro 클래스 JSON 직렬화 실패
```

**원인**: Spring MVC의 기본 Jackson ObjectMapper가 Avro 클래스를 JSON으로 변환하지 못함

### 임시 해결 (현재 상태)
- contract-hub 의존성 제거
- 순수 Kotlin DTO로 전환
- 향후 Spring Cloud Contract 기반 Contract Test로 명세 검증 예정

```kotlin
// 현재: 순수 DTO 사용
data class UserInternalDto(
    val id: Long,
    val email: String,
    val nickname: String,
    // ...
)
```

## 분석

### Avro를 REST API에 사용하는 방식

| 방식 | 설명 | 장점 | 단점 |
|-----|------|-----|------|
| **Avro JSON** | Avro 스키마 기반 JSON 직렬화 | 스키마 검증, 호환성 체크 | 설정 복잡 |
| **Avro Binary** | Avro 바이너리 포맷 (Content-Type: avro/binary) | 고성능, 압축 | 디버깅 어려움, 클라이언트 지원 필요 |
| **DTO 변환** | Avro → DTO 변환 후 JSON 반환 | 단순, 호환성 좋음 | 변환 로직 중복 |

### Avro JSON 직렬화 구현 방법

```kotlin
// 방법 1: Jackson Avro Module
@Configuration
class AvroJacksonConfig {
    @Bean
    fun avroMapper(): ObjectMapper {
        return ObjectMapper(AvroFactory()).apply {
            registerModule(AvroModule())
        }
    }
}

// 방법 2: Custom HttpMessageConverter
@Configuration
class AvroHttpMessageConverterConfig {
    @Bean
    fun avroHttpMessageConverter(): HttpMessageConverter<*> {
        return AvroHttpMessageConverter()
    }
}
```

### Avro Binary 통신 구현 방법

```kotlin
// Content-Type: application/avro
@GetMapping(value = ["/users/{id}"], produces = ["application/avro"])
fun getUserAvro(@PathVariable id: Long): ByteArray {
    val user = userService.findById(id)
    return AvroSerializer.serialize(user)
}
```

## 결정 사항

### 단기 (현재)
- **REST API**: 순수 DTO + JSON 사용
- **Kafka 이벤트**: Avro 스키마 + Schema Registry 사용
- **API 명세 검증**: Spring Cloud Contract 도입 예정

### 장기 (고도화)
- 내부 마이크로서비스 간 통신을 Avro 기반으로 전환 검토
- 성능 최적화가 필요한 고빈도 API부터 점진적 적용

## 고도화 작업 목록

### Phase 1: 기반 구축
- [ ] `platform-core`에 Avro HTTP 통신 자동설정 추가
  - AvroHttpMessageConverter 구현
  - Content-Type negotiation 지원 (JSON/Avro 선택적)
- [ ] contract-hub API 스키마 활용 방안 정리

### Phase 2: 파일럿 적용
- [ ] customer-service Internal API에 Avro 통신 적용
  - `Accept: application/avro` → Avro Binary 응답
  - `Accept: application/json` → JSON 응답 (기본값)
- [ ] 성능 벤치마크 (JSON vs Avro Binary)

### Phase 3: 확대 적용
- [ ] Order, Payment, Product 서비스 간 Internal API에 Avro 적용
- [ ] API Gateway에서 Avro → JSON 변환 계층 검토

## 기대 효과

| 항목 | JSON | Avro Binary |
|-----|------|-------------|
| **페이로드 크기** | 100% | ~30-50% |
| **직렬화 속도** | 기준 | 2-5x 빠름 |
| **스키마 검증** | 런타임 (선택적) | 컴파일 타임 |
| **하위 호환성** | 수동 관리 | Schema Registry 자동 관리 |

## 참고 자료

- [Apache Avro Specification](https://avro.apache.org/docs/current/spec.html)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Avro vs JSON vs Protobuf 비교](https://www.confluent.io/blog/avro-kafka-data/)

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|-----|---------|-------|
| 2025-11-27 | 최초 작성 | Claude Code |
