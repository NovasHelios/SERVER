# 데이터 저장·수정·삭제 무결성 테스트 보고서

## 개요

- 수행 일시: 2026-09-21 17:19 (KST)
- 실행 대상: `DataIntegrityIntegrationTest`
- 결과: 3건 통과, 실패 0건
- 환경: Java 21, Spring Boot 3.2.5, `test` 프로필, H2 인메모리 DB

## 검증 결과

| 구분 | 시나리오 | 기대 결과 | 결과 |
| --- | --- | --- | --- |
| 저장 | 동일 이메일 사용자 2건 저장 | DB unique 제약 위반 (`DataIntegrityViolationException`) | 통과 |
| 수정 | 사용자 이름·전화번호 수정 후 영속성 컨텍스트 초기화 및 재조회 | 수정값이 DB에 유지 | 통과 |
| 삭제 | 본인 계정 삭제 | 레코드는 유지되고 `isActive=false`로 소프트 삭제 | 통과 |

## 결론

사용자 데이터에 대해 이메일 중복 제약, 수정값 영속화, 소프트 삭제 정책이 H2 기반 실제 JPA 저장소에서 정상 동작했다.

## 범위 제한

- H2의 PostgreSQL 호환 모드에서 검증했으며 운영 PostgreSQL의 인덱스·트리거·동시성 조건은 포함하지 않는다.
- 토지, 신청, 찜, 채팅 간 FK 삭제 전파와 동시 업데이트 충돌은 별도 데이터 시나리오가 필요하다.

## 산출물

- 테스트 코드: `src/test/java/com/heilous/DataIntegrityIntegrationTest.java`
