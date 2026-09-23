# Kakao·VWorld 외부 API 실패 및 예외 테스트 보고서

## 개요

- 수행 일시: 2026-09-21 17:19 (KST)
- 실행 대상: `ExternalApiFailureIntegrationTest`
- 결과: 2건 통과, 실패 0건
- 환경: Java 21, Spring Boot 3.2.5, `test` 프로필, MockMvc

## 검증 결과

| 대상 | 시나리오 | 기대 결과 | 결과 |
| --- | --- | --- | --- |
| VWorld API | 서비스에서 외부 연동 실패(`EXTERNAL_API_ERROR`) 발생 | API 응답 502, 코드 `EXT_001` | 통과 |
| Kakao 주소 데이터 | 주소 객체가 없거나 법정동 코드(`b_code`)가 비어 있음 | `KAKAO_ADDRESS_NOT_FOUND` 발생 | 통과 |

## 결론

외부 API 호출 실패는 전역 예외 처리기를 통해 클라이언트에 502/`EXT_001` 형식으로 전달되며, Kakao 응답으로 PNU를 만들 수 없는 주소 데이터는 404/`EXT_002`로 구분할 수 있다.

## 범위 제한

- 실제 Kakao/VWorld 서버에 요청하지 않고, VWorld 서비스 실패를 Mock으로 재현했다. 따라서 인증키 오류, 실제 HTTP 4xx/5xx 본문, 연결 시간 초과의 실통신 검증은 포함하지 않는다.
- 현재 외부 호출 URL이 서비스 내부 상수로 고정돼 있어 로컬 stub 서버를 주입하기 어렵다. HTTP 클라이언트 또는 API base URL을 주입 가능하게 분리하면 timeout·비정상 응답을 결정적으로 테스트할 수 있다.

## 산출물

- 테스트 코드: `src/test/java/com/heilous/ExternalApiFailureIntegrationTest.java`
