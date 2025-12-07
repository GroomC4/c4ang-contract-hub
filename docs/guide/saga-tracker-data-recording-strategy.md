# 도메인 서비스 Saga Tracker 데이터 기록 전략

## 1. 목적
- Order/Product/Payment 등 물리적으로 분리된 MSA가 Saga 진행 상황을 어떻게 기록할지에 대한 기준을 정한다.
- `saga.tracker` 토픽 기반 중앙 추적과 서비스 로컬 DB 아카이브(Shadow Table) 방안을 비교하여 선택/병행 전략을 수립한다.

## 2. 공통 요구사항
1. **정합성**: 각 단계 기록은 서비스 로컬 트랜잭션(주 도메인 작업)과 동일한 경계 내에서 처리되어야 한다.
2. **감사 추적성**: 최소 30일 이상 전체 Saga 타임라인을 복원 가능해야 하며, Fail/Compensate 연동이 명확해야 한다.
3. **관측 가능성**: Grafana/Prometheus 대시보드와 Alert Rule에서 동일한 데이터를 사용할 수 있어야 한다.
4. **장애 복구**: Tracker 기록 누락 시 재처리(Reconcile/Replay)가 가능해야 하며, DLQ 또는 Outbox를 통해 멱등성이 확보되어야 한다.

## 3. 대안 비교

| 구분 | 도메인 서비스별 Shadow Table 유지 | 중앙 Saga Tracker Service + 전용 DB |
|------|--------------------------------|-----------------------------------|
| 데이터 저장 위치 | 각 서비스 DB에 `saga_tracker_history` 같은 동일 스키마 생성 | Tracker Service 전용 DB(PostgreSQL + Elastic) |
| 장점 | - 서비스별로 즉시 조회 가능<br>- 로컬 트랜잭션 내에서 Insert → 강한 정합성<br>- Kafka 장애 시에도 DB에는 기록됨 | - 모든 Saga 히스토리 중앙화, 탐색/검색 용이<br>- 스키마/비즈니스 룰 변경을 단일 서비스에서 제어<br>- Grafana/Alert, 감사 보고서, API 일원화 |
| 단점 | - 서비스 수만큼 스키마/마이그레이션 중복 관리<br>- Cross-domain Saga 조회가 어렵고 조인 비용 큼<br>- 공통 규칙 복제가 필요하여 Drift 위험 | - Tracker 서비스 장애 시 기록 지연 우려 (Outbox/Replay 필요)<br>- 추가 인프라(DB, Cache, API) 운영 비용<br>- 각 서비스에서 Tracker SDK 적용 필요 |
| 적합 시나리오 | - 도메인 단독으로 Saga 정보를 활용하는 특별 케이스<br>- 규제상 특정 데이터가 외부 DB로 나갈 수 없는 경우 | - 다수의 Saga/서비스가 공통 표준으로 운영될 때<br>- 감사/모니터링/알림 요구가 중앙 집중적일 때 (기본 선택) |

## 4. Hybrid 전략 제안

### 4.1 권장 기본: 중앙 Tracker Service
- 모든 서비스는 `saga.tracker` 토픽에 **직접** 이벤트를 발행한다 (SDK → Kafka Template).
- Tracker Service가 Kafka를 소비하여 전용 DB에 저장하고, REST/GraphQL API + Grafana Dashboard를 제공한다.
- 저장소는 초기엔 PostgreSQL을 사용하되, `docs/guide/saga-tracker-implementation-plan.md` 8장에 기술한 MongoDB 기반 NoSQL 옵션을 병행 검토한다.
- 운영·모니터링·감사 워크플로가 단일 뷰로 통합된다.

### 4.2 Shadow Table/Outbox 선택적 사용 (차후)
- 현재 릴리스에서는 직접 Kafka 발행만 사용하고, Outbox/Shadow Table은 후속 단계에서 도입한다.
- 도입 시 구조 예:
  ```
  saga_tracker_outbox (
    id UUID PK,
    saga_id,
    saga_type,
    step,
    status,
    order_id,
    metadata_json,
    recorded_at TIMESTAMP,
    published BOOLEAN DEFAULT FALSE
  )
  ```
- 트랜잭션 내에서 Insert 후, Outbox Processor가 Kafka에 Publish → 성공 시 `published=true`.
- 필요 시 뷰/물리 테이블로 노출하여 서비스별 빠른 조회 지원.

### 4.3 데이터 수명 주기
1. **서비스 DB**: (향후 Outbox 도입 시) Outbox 기록은 7~14일 보관 후 파티셔닝/압축.
2. **Kafka `saga.tracker`**: 30일 보존 (`docs/interface/kafka-event-specifications.md`).
3. **Tracker DB**: 90일 온프레미스 저장 후 S3/Glacier 같은 Cold Storage로 스냅샷.

## 5. 구현 지침
1. **공통 스키마**: `src/main/events/avro/saga/SagaTracker.avsc` 변경 시 SDK와 Tracker DB 스키마를 동시에 관리하도록 버전 정책 수립.
2. **SDK/라이브러리**:
   - `@Transactional` 경계 내에서 `SagaTrackerClient.recordStep()` 호출 → 즉시 Kafka 발행.
   - 멱등키: `eventId` or `sagaId + step + status`.
   - OpenTelemetry Trace ID를 metadata에 자동 주입.
   - Outbox 기반 전송은 추후 추가될 예정이므로 인터페이스 레이어를 분리해둔다.
3. **재처리**:
   - Kafka 재처리 필요 시 Tracker Service는 Offset rewind or DLQ 재소비를 지원.
   - Outbox Processor 기반 재전송은 도입 이후에만 적용하므로 현재는 Kafka 재시도/프로듀서 로그 중심으로 대응한다.
4. **거버넌스**:
   - 데이터 주체 서비스/소유자 명시.
   - 릴리즈 체크리스트에 “Saga Tracker 이벤트 필수 기록” 항목 추가 (`docs/interface/kafka-event-specifications.md` 체크리스트 연동).

## 6. 의사결정 요약
1. **기본 선택**: 중앙 `saga-tracker-service + 별도 DB` 방식을 표준으로 채택하여 감사/관측 요구사항을 충족한다.
2. **옵션**: 서비스 팀이 필요 시 동일한 Outbox/Shadow Table 구조를 활용해 로컬 보관 가능하되, 중앙 Tracker와의 데이터 정합성 검증을 위해 일일 비교 리포트를 생성한다.
3. **다음 액션**:
   - SDK 패키지 설계 및 Outbox 스키마 초안 배포.
   - Tracker Service 스키마/Helm Chart 정의 (`docs/guide/saga-tracker-implementation-plan.md`와 연계).
   - Shadow Table 사용 여부를 서비스별 설계 리뷰에서 명시.
