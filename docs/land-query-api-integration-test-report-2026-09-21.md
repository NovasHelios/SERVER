# 지도·토지 필터·토지 상세조회 API 통합 테스트 보고서

## 개요

- 수행 일시: 2026-09-21 22:51 (KST)
- 실행 대상: `LandQueryApiIntegrationTest`
- 결과: 6건 통과, 실패 0건, 오류 0건
- 환경: Java 21, Spring Boot 3.2.5, `test` 프로필, H2 인메모리 DB, MockMvc

## 범위

| 영역 | 엔드포인트 |
| --- | --- |
| 지도 조회 | `GET /api/lands`, `GET /api/lands/regions` |
| 토지 필터 | `POST /api/lands/filter` |
| 토지 상세조회 | `GET /api/lands/{landId}` |

## 검증 결과

| 영역 | 시나리오 | 기대 결과 | 결과 |
| --- | --- | --- | --- |
| 지도 조회 | 전체 토지 목록 조회 | 200, 토지 ID·주소·좌표(x/y)·거래유형 반환 | 통과 |
| 지도 조회 | 시도/시군구별 거래유형 통계 조회 | 200, 지역 및 거래유형별 건수 반환 | 통과 |
| 토지 필터 | 상태, 거래유형, 최소 가격, 시도 조건으로 조회 | 200, 조건이 서비스 계층에 정확히 전달되고 결과 반환 | 통과 |
| 토지 필터 | 잘못된 거래유형 enum 입력 | 400, `INVALID_FORMAT` 반환 | 통과 |
| 토지 상세조회 | 존재하는 토지 조회 | 200, 소유자·상태·이미지 등 상세 정보 반환 | 통과 |
| 토지 상세조회 | 존재하지 않는 토지 조회 | 404, `LAND_001` 반환 | 통과 |

## 결론

지도에 필요한 목록 좌표 및 지역 통계 응답, 복합 필터 역직렬화, 토지 상세 응답과 오류 형식이 HTTP 계층에서 정상 동작했다.

## 범위 제한

- `LandService`는 Mock으로 대체했으므로 실제 공간 범위 조건, 가격 범위, 정렬 및 JPA Specification 쿼리 결과는 이 보고서에 포함되지 않는다.
- 실제 PostgreSQL/PostGIS 및 지도 클라이언트 렌더링과의 연동은 별도 E2E 테스트가 필요하다.

## 산출물

- 테스트 코드: `
-
- src/test/java/com/heilous/LandQueryApiIntegrationTest.java`
- 결과 XML: `build/test-results/test/TEST-com.heilous.LandQueryApiIntegrationTest.xml`
