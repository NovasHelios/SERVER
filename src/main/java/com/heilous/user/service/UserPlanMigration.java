package com.heilous.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 기존 FREE/PRO 플랜 데이터를 OWNER로 일회성 변환하는 호환 마이그레이션이다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPlanMigration {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner migrateLegacyUserPlans() {
        return args -> {
            jdbcTemplate.execute("alter table users drop constraint if exists users_plan_check");
            int updated = jdbcTemplate.update(
                    "update users set plan = 'OWNER' where plan in ('FREE', 'PRO')");
            jdbcTemplate.execute("alter table users add constraint users_plan_check "
                    + "check (plan in ('OWNER', 'BUSINESS'))");
            if (updated > 0) log.info("기존 FREE/PRO 플랜 {}건을 OWNER로 변환했습니다.", updated);
        };
    }
}
