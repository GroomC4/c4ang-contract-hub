# Store Service API

K8s 클러스터 내부에서 매장 정보를 조회하는 API입니다.

## APIs

| API | 설명 | 주요 사용처 |
|-----|------|-----------|
| [Internal Store API](./internal-store-api.md) | 매장 정보 조회 | Product Service (상품 등록 시 스토어 검증) |

## 주요 사용자

- **Product Service**: 상품 등록 시 스토어 소유자 검증 및 스토어 이름 조회
- **Order Service**: 주문 시 스토어 정보 확인 (예정)

## 관련 문서

- [Internal Store API](./internal-store-api.md) - 스토어 조회 API 상세
- [API Flows 메인](../README.md) - 전체 API 목록
