# 사용자 권한 테스트 보고서

## 개요

- 수행 일시: 2026-09-21 17:19 (KST)
- 실행 대상: `UserAuthorizationIntegrationTest`
- 결과: 2건 통과, 실패 0건
- 환경: Java 21, Spring Boot 3.2.5, `test` 프로필, H2 인메모리 DB

## 검증 결과

| 대상 | 시나리오 | 기대 결과 | 결과 |
| --- | --- | --- | --- |
| 토지 신청 | USER 역할이 토지 신청 | `ACCESS_DENIED`, 신청 레코드 미생성 | 통과 |
| 토지 신청 | COMPANY 역할이 토지 신청 | 신청 레코드 생성 | 통과 |
| 계정 비활성화 | 일반 사용자가 다른 사용자 계정 비활성화 | `ACCESS_DENIED`, 대상 계정 활성 상태 유지 | 통과 |
| 계정 비활성화 | ADMIN이 다른 사용자 계정 비활성화 | 대상 계정 비활성화 | 통과 |

## 결론

검증한 서비스 경로에서 COMPANY 전용 토지 신청과 ADMIN의 타인 계정 비활성화 권한이 의도대로 동작했다. JWT 미인증 요청의 401 응답은 기존 주요 API 테스트에서 별도로 검증했다.

## 범위 제한

- 토지 등록/승인, 채팅, 찜 등 나머지 역할별 API는 이번 보고서의 실행 범위에 포함되지 않는다.
- `WishService`는 코드상 COMPANY 역할을 직접 확인하지 않으므로, 찜 기능의 역할 제한은 별도 보완 검토가 필요하다.

## 산출물

- 테스트 코드: `src/test/java/com/heilous/UserAuthorizationIntegrationTest.java`
