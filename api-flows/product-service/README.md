# Product Service API

K8s 클러스터 내부에서 상품 정보를 조회하는 API입니다.

## APIs

| API | 설명 | 주요 사용처 |
|-----|------|-----------|
| [Internal Product API](./internal-product-api.md) | 상품 정보 조회 | Order Service (주문 생성 시 상품 조회) |

## 주요 사용자

- **Order Service**: 주문 생성 시 상품 정보 조회 (가격, 스토어 정보)
- **Payment Service**: 결제 시 상품 유효성 검증

## 관련 문서

- [Internal Product API](./internal-product-api.md) - 상품 조회 API 상세
- [API Flows 메인](../README.md) - 전체 API 목록
