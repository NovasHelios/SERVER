package com.heilous.land.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 용도지역지구 코드 CSV 를 로드해 코드→명칭 매핑을 제공하는 서비스
 * CSV 위치: resources/data/용도지역지구구분 조회자료 - Sheet0.csv
 * 형식: 코드값,코드값의미,비고 (첫 번째 행 헤더 제외)
 */
@Slf4j
@Service
public class LandCodeService {

    private final Map<String, String> codeMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("data/용도지역지구구분 조회자료 - Sheet0.csv");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first) { first = false; continue; } // 헤더 스킵
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 2) {
                        String code = parts[0].trim();
                        String name = parts[1].trim();
                        if (!code.isEmpty()) {
                            codeMap.put(code, name);
                        }
                    }
                }
            }
            log.info("용도지역지구 코드 {}건 로드 완료", codeMap.size());
        } catch (Exception e) {
            log.error("용도지역지구 코드 CSV 로드 실패: {}", e.getMessage());
        }
    }

    /**
     * 코드에 해당하는 명칭 반환. 없으면 null.
     */
    public String getName(String code) {
        if (code == null) return null;
        return codeMap.get(code.trim());
    }
}
