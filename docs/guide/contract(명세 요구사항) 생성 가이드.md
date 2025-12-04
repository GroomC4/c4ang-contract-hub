# Contract 패키지 구조 가이드

## 개요

이 문서는 Consumer-Driven Contract (CDC) 패턴에 따른 Contract 파일 관리 구조를 정의합니다.
**전략: Consumer별 독립 Contract**를 채택합니다.

---

## 디렉토리 구조

```
{service-name}/
└── src/test/resources/contracts/
    ├── internal/                           # Internal API Contracts
    │   ├── {consumer-service-1}/           # Consumer별 디렉토리
    │   │   ├── {api-name}.groovy
    │   │   └── {api-name}.groovy
    │   ├── {consumer-service-2}/
    │   │   └── {api-name}.groovy
    │   └── {consumer-service-n}/
    │       └── {api-name}.groovy
    │
    └── external/                           # External API Contracts (Optional)
        └── {consumer-app}/
            └── {api-name}.groovy
```

### 예시: Store Service

```
store-service/
└── src/test/resources/contracts/
    └── internal/
        ├── order-service/
        │   └── getStoreById.groovy         # name, address 필드만 검증
        ├── product-service/
        │   └── getStoreById.groovy         # name, category 필드만 검증
        └── payment-service/
            └── getStoreById.groovy         # name, bankAccount 필드만 검증
```

---

## 네이밍 컨벤션

### 디렉토리명
- **Consumer 서비스명**: kebab-case 사용 (예: `order-service`, `product-service`)
- **internal/external 구분**: Internal API와 External API를 최상위에서 분리

### 파일명
- **Contract 파일**: `{동사}{리소스}.groovy` 형식 (예: `getStoreById.groovy`, `createOrder.groovy`)
- camelCase 사용

---

## Contract 작성 규칙

### 1. Consumer가 필요한 필드만 정의
```groovy
// 좋은 예: Product Service가 필요한 필드만
Contract.make {
    request {
        method GET()
        url "/internal/v1/stores/123"
    }
    response {
        status OK()
        body([
            id: $(anyUuid()),
            name: $(anyNonBlankString()),
            category: $(anyNonBlankString())
        ])
    }
}
```

```groovy
// 나쁜 예: 모든 필드를 포함 (Union Contract)
Contract.make {
    response {
        body([
            id: $(anyUuid()),
            name: $(anyNonBlankString()),
            address: $(anyNonBlankString()),      // Product Service는 불필요
            category: $(anyNonBlankString()),
            bankAccount: $(anyNonBlankString())   // Product Service는 불필요
        ])
    }
}
```

### 2. Internal API 경로 표준
```
/internal/v1/{resource}/{id}
/internal/v1/{resource}
```

### 3. 공통 필드가 많을 경우 Matcher 재사용
```groovy
// 공통 Matcher 정의 (별도 파일 또는 상단에 정의)
def storeBaseMatcher = [
    id: $(anyUuid()),
    name: $(anyNonBlankString())
]

// Consumer Contract에서 확장
Contract.make {
    response {
        body(storeBaseMatcher + [
            category: $(anyNonBlankString())
        ])
    }
}
```

---

## 역할 및 책임

| 역할 | 책임 |
|------|------|
| **Consumer** | Contract 작성, PR 생성 |
| **Producer** | Contract 검증, Stub 생성/배포 |

### 워크플로우
```
1. Consumer가 contracts/{consumer-name}/ 에 Contract 작성
2. Producer repo에 PR 생성
3. Producer CI에서 Contract 검증 (./gradlew contractTest)
4. 검증 통과 시 Merge → Stub 자동 배포
5. Consumer가 Stub을 사용하여 통합 테스트
```

---

## Contract 파일 템플릿

```groovy
package contracts.internal.{consumerName}

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "{consumerName}_{apiName}"
    description """
        Consumer: {consumer-service-name}
        Purpose: {API 호출 목적}
    """

    request {
        method {HTTP_METHOD}()
        url "/{path}"
        headers {
            contentType applicationJson()
        }
    }

    response {
        status {STATUS_CODE}()
        headers {
            contentType applicationJson()
        }
        body([
            // Consumer가 필요한 필드만 정의
        ])
    }
}
```

---

## 주의사항

1. **Union Contract 금지**: 모든 필드를 포함하는 통합 Contract는 생성하지 않음
2. **Consumer 중심**: Contract는 항상 Consumer가 필요로 하는 것만 정의
3. **독립성 유지**: 각 Consumer의 Contract는 다른 Consumer에 영향을 주지 않음
4. **버전 관리**: API 버전이 변경되면 새 Contract 작성 (기존 Contract 유지)
