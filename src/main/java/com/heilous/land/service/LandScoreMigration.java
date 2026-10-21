package com.heilous.land.service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 분석 실패 시 총점을 미산정(null)으로 저장할 수 있게 기존 DB 제약을 완화한다. */
@Component
public class LandScoreMigration {

    @Bean
    ApplicationRunner allowNullLandScore(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.execute("alter table lands alter column land_score drop not null");
    }
}
