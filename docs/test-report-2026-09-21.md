# 주요 API 테스트 보고서

## 개요

- 수행 일시: 2026-09-21 17:12 (KST)
- 대상: 인증, 토지 조회/필터, 찜 API의 정상 및 오류 응답
- 실행 명령: `./gradlew test`
- 결과: 성공 (8건 통과, 실패 0건, 오류 0건, 제외 0건)

## 테스트 환경

- Java 21, Spring Boot 3.2.5
- `test` 프로필
- H2 인메모리 데이터베이스 (PostgreSQL 호환 모드)
- MockMvc를 통한 HTTP 요청/응답 검증
- `AuthService`, `LandService`, `WishService`는 Mockito Mock으로 대체

## 결과 요약

| 구분 | 건수 | 결과 |
| --- | ---: | --- |
| 애플리케이션 컨텍스트 기동 | 1 | 통과 |
| 주요 API 요청 | 7 | 통과 |
| 합계 | 8 | 통과 |

## 주요 API 검증 내역

| API | 시나리오 | 기대 결과 | 결과 |
| --- | --- | --- | --- |
| `POST /api/auth/login` | 유효한 이메일·비밀번호 | 200, access token 및 역할 반환 | 통과 |
| `POST /api/auth/login` | 이메일 형식/필수값 오류 | 400, `VALIDATION_ERROR` | 통과 |
| `POST /api/auth/login` | 잘못된 자격증명 | 401, `AUTH_001` | 통과 |
| `GET /api/lands` | 비로그인 공개 목록 조회 | 200, 배열 응답 | 통과 |
| `POST /api/lands/filter` | 잘못된 `status` enum 값 | 400, `INVALID_FORMAT` | 통과 |
| `GET /api/wishes` | 인증 헤더 없음 | 401, `AUTH_009` | 통과 |
| `POST /api/wishes/42` | 유효한 COMPANY JWT로 찜 등록 | 200, 성공 메시지 | 통과 |

## 확인된 사항 및 제한

- 요청 라우팅, JWT 인증 필터, 입력값 검증, 전역 예외 응답 형식을 HTTP 계층에서 검증했다.
- 서비스 계층은 Mock 처리했으므로 실제 PostgreSQL/Redis, 이메일·VWorld·AI 등 외부 연동과 서비스 내부 비즈니스 규칙은 본 결과에 포함되지 않는다.
- 로컬 서버를 `test` 프로필로 직접 기동할 때 `.env`의 MySQL JDBC URL이 테스트 프로필의 H2 설정을 덮어쓰며, PostgreSQL 드라이버와 충돌해 기동에 실패했다. 따라서 이번 결과는 H2 격리 테스트 컨텍스트 기준이다.

## 산출물

- 테스트 코드: `src/test/java/com/heilous/MajorApiRequestIntegrationTest.java`
- 테스트 결과 XML: `build/test-results/test/TEST-com.heilous.MajorApiRequestIntegrationTest.xml`
