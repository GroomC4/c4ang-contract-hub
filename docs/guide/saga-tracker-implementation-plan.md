# Saga Tracker 구현 및 Grafana 모니터링 계획

## 1. 배경
- `docs/index/documents-index.md`와 `docs/interface/kafka-event-specifications.md`에서 `saga.tracker` 토픽, 파티션/보존 정책(3 partitions, RF3, 30일), `saga-tracker-service` 컨슈머 그룹, 보상 단계별 `saga.tracker` 기록 의무가 이미 표준으로 명시되어 있다.
- 기존 Order Creation/Payment Completion Saga는 이벤트 코레오그래피 기반이며, `src/main/events/avro/saga/SagaTracker.avsc` 스키마가 `{sagaId, sagaType, step, status, metadata, recordedAt}` 필드를 정의한다.
- 신규 문서는 해당 표준을 구현 관점에서 구체화하고, K8s 내 Grafana를 통한 모니터링/알림 계획을 포함한다.

## 2. 목표 및 범위
1. 모든 도메인 서비스(Product, Order, Payment 등)가 Saga 단계 및 보상 시점을 `saga.tracker`에 기록하도록 SDK/API 계약을 수립한다.
2. Tracker Service가 Kafka → 중앙 저장소(PostgreSQL/Elastic) → Observability(Grafana) 파이프라인을 구성하여 30일 이상의 감사 가능성을 제공한다.
3. 운영자가 Grafana에서 실시간으로 실패율, 보상 지연, Consumer Lag, Alert 이력을 확인할 수 있도록 Dashboards/Alert Rule을 자동 배포한다.

## 3. 아키텍처 개요
```
Domain Services ─┬─(Tracker SDK)→ Kafka `saga.tracker`
                 │
                 └─(Failure/Compensation Events)→ 기타 Saga 토픽

Kafka `saga.tracker`
    ↓ (Consumer Group: saga-tracker-service)
Saga Tracker Service
    ├─ PostgreSQL: saga_instance / saga_steps
    ├─ Elasticsearch(or OpenSearch): Full-text 검색/Analytics
    ├─ Redis (Optional): 최근 Saga 캐시 & 타임아웃 워커
    ├─ Micrometer + Prometheus Exporter
    └─ REST/GraphQL API + Admin UI
```

### 3.1 Producer 측 (Domain Services)
- 공통 SDK 제공: `SagaTrackerClient.recordStep(step, status, orderId, metadataMap)`만 호출하면 Avro Serializer와 Kafka Template가 처리하며 **현재는 직접 Kafka에 발행**한다.
- Step 네이밍 가이드: `STOCK_RESERVATION`, `PAYMENT_INITIALIZATION`, `PAYMENT_COMPLETION`, `ORDER_CONFIRMATION`, `COMPENSATION_{domain}` 등 문서화.
- Metadata 필드에 OpenTelemetry Trace/Span ID, 주요 파라미터(orderAmount, failureReason 등) JSON 저장.
- Outbox 전략은 차후(예: 정확한 한 번 전송, 대량 재처리 필요 시) 도입하며, 현재 설계에는 포함하지 않는다.

### 3.2 Kafka 구성
- `saga.tracker` 토픽 (3 partitions / RF3 / retention.ms=2592000000). 파티션 키는 `sagaId`로 동일 Saga의 순서를 보장.
- 필요한 경우 `saga.tracker.dlt` 추가하여 파싱 실패 이벤트를 격리, Tracker Service는 DLQ를 모니터링하여 데이터 품질 이슈를 조기 감지.

### 3.3 Saga Tracker Service
- Spring Boot + Kotlin 권장 (리포 내 표준). Consumer는 `saga-tracker-service` 그룹 ID 사용.
- Exactly-once: Kafka Transactional Consumer 또는 Upsert 기반 멱등 처리 (`eventId`/`recordedAt` unique constraint).
- DB 모델:
  - `saga_instance(saga_id PK, saga_type, current_status, started_at, updated_at, order_id, last_step, last_trace_id)`
  - `saga_steps(id PK, saga_id FK, step, status, recorded_at, producer_service, metadata_json)`
- API:
  - `GET /sagas/{sagaId}`
  - `GET /sagas?orderId=&status=&type=`
  - `GET /sagas/{sagaId}/steps`
- Admin UI(선택) 또는 Grafana Table 패널로 대체. 런북 링크/Trace Jump 기능 제공.

## 4. Grafana 모니터링 및 Alert 설계

### 4.1 Prometheus Metrics
- Micrometer 지표 목록:
  - `saga_active_total` (Gauge) : 상태별 active saga 수
  - `saga_failed_total` / `saga_compensated_total` (Counter)
  - `saga_compensation_duration_seconds` (Histogram) : 실패→보상 완료 소요 시간
  - `saga_tracker_consumer_lag` (Gauge) : Kafka Exporter/Burrow metrics import
  - `saga_tracker_api_latency_seconds` (Histogram) : API 응답 속도
- `ServiceMonitor`/`PodMonitor` CR을 Helm values에 포함하여 `/actuator/prometheus` 스크랩.

### 4.2 Grafana Dashboard
- 자동 프로비저닝: Helm Chart의 `grafanaDashboard` ConfigMap으로 `saga-tracker-dashboard.json` 등록.
- 패널 아이디어:
  1. 상태 분포(Started/In-progress/Failed/Compensated) Stacked Bar
  2. 최근 1시간 Saga Step 타임라인 (Table + Sparkline)
  3. Compensation Duration 95p/99p Line Chart
  4. `saga-tracker-service` Lag & Offset 패널
  5. Alert/Incident Feed (Slack/PagerDuty Webhook 로그)
  6. API Error Rate/Latency 패널

### 4.3 Alert Rule (Grafana Alerting)
- **Failure Spike**: `increase(saga_failed_total[5m]) > failure_baseline * 3σ` → Slack/PagerDuty.
- **Compensation Timeout**: `saga_failed_total - saga_compensated_total{lag>5m} > 0` 일 때 Critical.
- **Consumer Lag**: `saga_tracker_consumer_lag > 1000` 10분 지속 시 Warning.
- AlertRule YAML은 Helm에서 `PrometheusRule`로 정의하고 Grafana Contact Point와 연동.

## 5. 운영 Runbook 요약
1. Grafana Alert 수신 → Dashboard에서 해당 시간대 패널 확인.
2. 문제 Saga `sagaId` 식별 → Tracker API (`GET /sagas/{sagaId}`) 호출 → 단계별 상태 확인.
3. Metadata에 포함된 `traceId`를 Jaeger/Tempo에서 조회하여 근본 원인 파악.
4. 필요 시 도메인 서비스 로그/Outbox 상태 확인.
5. 보상 미실행 건은 `saga-tracker-service`에서 수동 재처리(Replay) 또는 DLQ 재전송.
6. Producer 서비스는 SDK 재시도/로그만 확인하면 되며, Outbox 재처리는 차후 단계에서 추가한다.

## 6. 로드맵
| 주차 | 목표 |
|------|------|
| 1주차 | SDK 스펙/Step 네이밍 확정, 직접 Kafka 발행 방식 공통화 |
| 2주차 | Tracker Service Skeleton (Consumer + DB schema + API) 개발, Helm Chart 초안 |
| 3주차 | Grafana Dashboard/Alert Rule 작성, Prometheus 연동, Chaos 테스트로 전체 파이프라인 검증 |
| 4주차 | 서비스별 SDK 적용 릴리즈, 모니터링 튜닝, Runbook 정리 |
| 5주차 | 운영 안정화, Elastic/Redis 연동 및 추가 SLA 지표 구축, Outbox 도입 준비 |

## 7. 결론
- 기존 문서의 표준을 실행 가능한 수준으로 구체화했으며, K8s Grafana/Prometheus 스택과 연동해 실시간 추적·알림 체계를 구축한다.
- 각 서비스는 트래커 이벤트 발행만 책임지고, 중앙 `saga-tracker-service`가 저장·조회·모니터링을 담당함으로써 감사 요구사항과 운영 편의성을 동시에 확보한다.

## 8. NoSQL 저장소 검토 및 프로젝트 기술 스택

### 8.1 RDB vs NoSQL 비교
| 항목 | PostgreSQL (기존 안) | NoSQL (예: MongoDB/Document DB) |
|------|--------------------|--------------------------------|
| 데이터 모델 | 정규화된 `saga_instance` + `saga_steps` 테이블, JOIN 필요 | Saga 단위 JSON 도큐먼트(단계 배열 포함)로 직관적 조회 |
| 쓰기 패턴 | 단계별 INSERT/UPDATE (트랜잭션 강점) | Append/Upsert, 대용량 이벤트 저장에 유리 |
| 조회패턴 | `sagaId/orderId` 기반 정확한 검색 | `sagaId`, `status`, 기간 필터링, `metadata` 내 필드 검색이 용이 |
| 확장성 | 수직 확장 + 샤딩 필요 | 수평 확장 쉬움 (샤드 키=sagaId) |
| 복잡도 | 기존 운영 경험 풍부 | 새 인프라 구축/운영 필요, 트랜잭션/조인 제약 |
| 감사지원 | ACID, FK 제약 등으로 강함 | Document 단위로 감사 가능, 다만 Schema 관리 필요 |

결론: 장기적으로 단계 수가 많고 `metadata` 구조가 가변적인 점을 감안하면 Document 기반 저장소가 운영 효율을 높일 수 있다. 초기엔 PostgreSQL을 유지하되, NoSQL 전환을 위한 설계도 병행한다.

### 8.2 권장 NoSQL 아키텍처 (MongoDB 예시)
```
Kafka `saga.tracker`
    ↓
Saga Tracker Service (Spring Boot 3.x, Kotlin, Coroutines)
    ↓
MongoDB Replica Set (3 nodes)
    ├─ Collection: saga_instances
    └─ TTL Index / Secondary Indexes
```

**도큐먼트 구조 예시**
```json
{
  "_id": "saga-{uuid}",
  "sagaId": "order-123",
  "sagaType": "ORDER_CREATION",
  "orderId": "ORD-123",
  "currentStatus": "COMPENSATED",
  "startedAt": "2025-01-02T01:23:45Z",
  "updatedAt": "2025-01-02T01:25:00Z",
  "steps": [
    {"step": "STOCK_RESERVATION", "status": "FAILED", "recordedAt": "...", "metadata": {...}},
    {"step": "ORDER_CONFIRMATION", "status": "COMPENSATED", "recordedAt": "..."}
  ],
  "traceIds": ["abc123", "def456"],
  "metadata": {"channel": "WEB", "failureReason": "OUT_OF_STOCK"}
}
```

### 8.3 기술 스택 제안
- **애플리케이션**: Kotlin + Spring Boot 3.2, Spring Kafka, Spring Data MongoDB, Coroutines/Flow 기반 비동기 처리.
- **빌드/테스트**: Gradle Kotlin DSL, Testcontainers (Kafka + Mongo)로 통합 테스트.
- **Observability**: Micrometer Prometheus Registry, Sleuth/Otel auto-config for trace propagation.
- **Infra**:
  - MongoDB ReplicaSet (3 Pods) + PersistentVolume, Helm 차트 운영.
  - Kubernetes Secret/ConfigMap으로 연결 문자열/사용자 관리.
  - MongoDB TTL Index (`updatedAt` 기반 120일) + Secondary Index (`sagaId`, `orderId`, `currentStatus`, `steps.step`).
- **Schema 관리**: JSON Schema 문서화 및 Spring Validator로 단계 metadata 검증.
- **Migration Plan**:
  1. v1: PostgreSQL 저장 (현재 플랜)
  2. v1.5: MongoDB 병행 저장(dual-write) 및 리포트 로직 이관
  3. v2: MongoDB 단일 소스, PostgreSQL는 보조 아카이브로 축소

### 8.4 선택 기준
- Saga 단계/Metadata가 빈번히 변경되고, JSON 기반 질의가 많으며, Grafana/Analytics에서 Document Drill-down을 자주 한다면 MongoDB 도입을 권장.
- 금융 규제나 강한 JOIN 필요성이 크다면 PostgreSQL을 유지하면서 MongoDB는 캐시/검색 용도로 제한.

### 8.5 다음 행동 아이템
1. POC: Spring Boot Tracker Service에 Spring Data MongoDBRepository 추가, kafka-consumer → mongo 저장 경로 검증.
2. Ops: MongoDB Helm 차트 배포(+TLS/백업), Prometheus MongoDB Exporter 연동.
3. 코드: Repository 추상화 계층(`SagaRepository` 인터페이스)을 정의하여 RDB/NoSQL 양쪽 구현체를 쉽게 교체 가능하도록 설계.
