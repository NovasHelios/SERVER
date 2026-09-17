-- PostGIS 익스텐션 활성화
-- docker-entrypoint-initdb.d 에 의해 DB 생성 직후 자동 실행됩니다.
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- 공간 인덱스 함수 확인
SELECT PostGIS_Version();
