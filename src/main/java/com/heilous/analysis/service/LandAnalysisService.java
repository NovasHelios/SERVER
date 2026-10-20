package com.heilous.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.analysis.dto.DevelopmentScoreResponse;
import com.heilous.analysis.dto.LandPriceHistoryResponse;
import com.heilous.analysis.dto.LandPriceHistoryResponse.YearlyPrice;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.entity.Land;
import com.heilous.land.entity.LandEtc;
import com.heilous.land.entity.LandZone;
import com.heilous.land.repository.LandRepository;
import com.heilous.vworld.dto.PossessionAttrResponse;
import com.heilous.vworld.service.VWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LandAnalysisService {

    private final LandRepository landRepository;
    private final VWorldService vWorldService;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_YEARS = 6;

    // ═══════════════════════════════════════════════════════════
    // 1. 개발가능성 분석 (캐시 우선)
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public DevelopmentScoreResponse analyzeDevelopment(Long landId) {
        Land land = getLandWithDetails(landId);

        // 캐시 히트
        if (land.getDevelopmentScoreCache() != null) {
            try {
                DevelopmentScoreResponse cached = objectMapper.readValue(land.getDevelopmentScoreCache(), DevelopmentScoreResponse.class);
                // road_side_code가 있으면 캐시의 roadScore를 재계산값으로 보정
                Integer roadFromCode = calcRoadScore(land.getRoadSideCode());
                if (roadFromCode != null && !roadFromCode.equals(cached.getRoadScore())) {
                    // 캐시 무효화 후 재분석
                    land.updateDevelopmentScoreCache(null);
                } else {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("개발가능성 캐시 역직렬화 실패, 재분석: {}", e.getMessage());
            }
        }
        }

        String etcCodesStr = land.getLandEtcs().stream()
                .map(LandEtc::getCode).collect(Collectors.joining(","));
        String zoneCodesStr = land.getLandZones().stream()
                .map(LandZone::getCode).collect(Collectors.joining(","));

        String etcNamesStr = land.getLandEtcs().stream()
                .map(e -> e.getCodeName() != null ? e.getCodeName() : e.getCode())
                .collect(Collectors.joining(", "));
        String zoneNamesStr = land.getLandZones().stream()
                .map(z -> z.getCodeName() != null ? z.getCodeName() : z.getCode())
                .collect(Collectors.joining(", "));

        Double evalArea = land.getDesiredArea() != null ? land.getDesiredArea() : land.getArea();
        int areaScore = calcAreaScore(evalArea);
        Integer regulationScore = calcRegulationScore(etcCodesStr);
        Integer zoneScore = calcZoneScore(land.getPrposAreaCode(), zoneCodesStr);

        // 도로접근성: road_side_code 있으면 직접 계산, 없으면 AI 판단
        Integer roadScoreFromCode = calcRoadScore(land.getRoadSideCode());

        AiScoreResult aiResult = callAiForScores(land, areaScore, regulationScore, zoneScore,
                etcNamesStr, zoneNamesStr, roadScoreFromCode);

        boolean disqualified = aiResult.slopeScore == null || aiResult.roadScore == null
                || regulationScore == null || zoneScore == null;

        Integer totalScore = null;
        String grade;
        if (!disqualified) {
            totalScore = aiResult.sunlightScore + aiResult.slopeScore + areaScore
                    + aiResult.roadScore + regulationScore + zoneScore;
            grade = toGrade(totalScore);
        } else {
            grade = "부적합";
        }

        DevelopmentScoreResponse result = DevelopmentScoreResponse.builder()
                .landId(land.getId())
                .address(land.getAddress())
                .sunlightScore(aiResult.sunlightScore)
                .slopeScore(aiResult.slopeScore)
                .areaScore(areaScore)
                .roadScore(aiResult.roadScore)
                .roadSideCode(land.getRoadSideCode())
                .regulationScore(regulationScore)
                .zoneScore(zoneScore)
                .totalScore(totalScore)
                .grade(grade)
                .disqualified(disqualified)
                .landCategory(land.getLcCodeNm())
                .area(land.getArea())
                .desiredArea(land.getDesiredArea())
                .averageSolarIrradiance(land.getAverageSolarIrradiance())
                .prposAreaName(land.getPrposAreaName())
                .zoneConflict(land.getPrposAreaCnflcAtNm())
                .zoneNames(zoneNamesStr.isBlank() ? null : zoneNamesStr)
                .etcNames(etcNamesStr.isBlank() ? null : etcNamesStr)
                .rationale(aiResult.rationale)
                .build();

        // 캐시 저장
        try {
            land.updateDevelopmentScoreCache(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("개발가능성 캐시 저장 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 캐시 무효화 (토지 수정 시 호출)
     */
    @Transactional
    public void invalidateCache(Long landId) {
        landRepository.findById(landId).ifPresent(Land::invalidateAiCache);
    }

    // ── 개발 가능 면적 점수 (10점 만점) ─────────────────────────
    private int calcAreaScore(Double area) {
        if (area == null) return 2;
        if (area >= 3000) return 10;
        if (area >= 2000) return 8;
        if (area >= 1000) return 6;
        if (area >= 500)  return 4;
        return 2;
    }

    // ── 토지이용규제 점수 (20점 만점) ──
    private Integer calcRegulationScore(String etcCodesStr) {
        if (etcCodesStr == null || etcCodesStr.isBlank()) return 20;
        Set<String> codes = Arrays.stream(etcCodesStr.split(","))
                .map(String::trim).collect(Collectors.toSet());
        boolean disqualify = codes.stream().anyMatch(c ->
                c.startsWith("UBD") || c.startsWith("FA") || c.startsWith("GF") || c.startsWith("UBB"));
        if (disqualify) return null;
        if (codes.size() >= 3) return 7;
        if (!codes.isEmpty()) return 14;
        return 20;
    }

    // ── 용도지역·용도지구 점수 (15점 만점) ──
    private Integer calcZoneScore(String prposAreaCode, String zoneCodesStr) {
        if (prposAreaCode != null) {
            if (prposAreaCode.startsWith("UQA02") || prposAreaCode.startsWith("UQA03")
                    || prposAreaCode.startsWith("UQA04")) return null;
            if (prposAreaCode.startsWith("UQA01")) return 15;
        }
        boolean hasRestrictiveZone = false;
        if (zoneCodesStr != null && !zoneCodesStr.isBlank()) {
            Set<String> zoneCodes = Arrays.stream(zoneCodesStr.split(","))
                    .map(String::trim).collect(Collectors.toSet());
            hasRestrictiveZone = zoneCodes.stream().anyMatch(c ->
                    c.startsWith("UQG") || c.startsWith("UQH") || c.startsWith("UQI"));
        }
        if (prposAreaCode == null && (zoneCodesStr == null || zoneCodesStr.isBlank())) return 8;
        if (hasRestrictiveZone) return 3;
        return 12;
    }

    // ── 일사량 점수 (25점 만점, NASA POWER) ──
    private int calcSunlightScore(Double solarIrradiance) {
        if (solarIrradiance == null) return 18;
        if (solarIrradiance >= 4.5) return 25;
        if (solarIrradiance >= 4.0) return 22;
        if (solarIrradiance >= 3.5) return 18;
        if (solarIrradiance >= 3.0) return 12;
        return 5;
    }

    // ── 도로접근성 점수 (10점 만점, road_side_code 기반) ──
    // 1=광대한면, 2=광대소각, 3=중로한면, 4=중로소각, 5=소로한면
    // 6=소로소각, 7=세로한면, 8=세로가, 9=맹지
    private Integer calcRoadScore(String roadSideCode) {
        if (roadSideCode == null || roadSideCode.isBlank()) return null; // 데이터 없음 → AI 판단
        return switch (roadSideCode.trim()) {
            case "1" -> 10; // 광대한면
            case "2" -> 8;  // 광대소각
            case "3" -> 6;  // 중로한면
            case "4", "5" -> 4; // 중로소각, 소로한면
            case "6", "7" -> 3; // 소로소각, 세로한면
            case "8" -> 2;  // 세로가
            case "9" -> null; // 맹지 → 부적합
            default -> null;
        };
    }

    // ── AI 호출 ──
    private AiScoreResult callAiForScores(Land land, int areaScore, Integer regulationScore,
                                           Integer zoneScore, String etcCodes, String zoneCodes,
                                           Integer roadScoreFromCode) {
        int sunlightScore = calcSunlightScore(land.getAverageSolarIrradiance());
        String prompt = buildAiPrompt(land, areaScore, regulationScore, zoneScore, etcCodes, zoneCodes,
                sunlightScore, roadScoreFromCode);
        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .system("You are a professional land suitability analyst. Always respond strictly in the requested format.")
                    .user(prompt)
                    .call()
                    .content();
            return parseAiResponse(response, sunlightScore, roadScoreFromCode);
        } catch (Exception e) {
            log.warn("AI 분석 호출 실패: {}", e.getMessage());
            return new AiScoreResult(sunlightScore, 13, roadScoreFromCode != null ? roadScoreFromCode : 6, "AI 분석을 수행할 수 없습니다.");
        }
    }

    private String buildAiPrompt(Land land, int areaScore, Integer regulationScore,
                                  Integer zoneScore, String etcCodes, String zoneCodes,
                                  int sunlightScore, Integer roadScoreFromCode) {
        String slopeStr = land.getAverageSlope() != null ? String.format("%.2f", land.getAverageSlope()) : "N/A (use regional estimate)";
        String solarStr = land.getAverageSolarIrradiance() != null ? String.format("%.3f kWh/m²/day", land.getAverageSolarIrradiance()) : "N/A";
        String roadInfo = roadScoreFromCode != null
                ? "Road Side Code: " + land.getRoadSideCode() + " → Pre-calculated Road Score: " + roadScoreFromCode + " / 10 (DO NOT re-evaluate)"
                : "Road Side Code: N/A (evaluate based on region)";

        String criteriaSection = roadScoreFromCode != null
                ? "[Scoring Criteria — evaluate ONLY Slope]\n1. Slope (max 20 pts): 0~5°:20 / 5~10°:17 / 10~15°:13 / 15~20°:8 / 20~25°:3 / >25°:null"
                : "[Scoring Criteria — evaluate ONLY these 2]\n1. Slope (max 20 pts): 0~5°:20 / 5~10°:17 / 10~15°:13 / 15~20°:8 / 20~25°:3 / >25°:null\n2. Road Accessibility (max 10 pts): direct:10 / <50m:8 / 50~100m:6 / 100~200m:3 / >200m:null";

        String roadScoreField = roadScoreFromCode != null
                ? "ROAD_SCORE: " + roadScoreFromCode
                : "ROAD_SCORE: <number or null>";

        return String.format("""
                You are a land suitability analyst for solar panel installation projects in South Korea.
                Based on the land information below, evaluate the criteria and provide a brief one-line rationale for each.

                [Land Information]
                Address: %s / Land Category: %s / Actual Area: %.1f sqm / Desired Area: %s sqm
                Region: %s %s / Zoning: %s / Zoning Conflict: %s
                District Zones: %s / Other Regulations: %s
                Average Slope: %s degrees / Average Solar Irradiance: %s
                %s

                [Pre-calculated Scores]
                Solar Irradiance Score: %d / 25 / Developable Area Score: %d / 10
                Land Use Regulation Score: %s / 20 / Zoning Score: %s / 15

                %s

                Respond ONLY in this exact format:
                SLOPE_SCORE: <number or null>
                %s
                RATIONALE_SUNLIGHT: <one-line reason in Korean>
                RATIONALE_SLOPE: <one-line reason in Korean>
                RATIONALE_AREA: <one-line reason in Korean>
                RATIONALE_ROAD: <one-line reason in Korean>
                RATIONALE_REGULATION: <one-line reason in Korean>
                RATIONALE_ZONE: <one-line reason in Korean>
                """,
                nullSafe(land.getAddress()), nullSafe(land.getLcCodeNm()),
                land.getArea() != null ? land.getArea() : 0.0,
                land.getDesiredArea() != null ? String.valueOf(land.getDesiredArea()) : "N/A",
                nullSafe(land.getRegionSido()), nullSafe(land.getRegionSigungu()),
                nullSafe(land.getPrposAreaName()), nullSafe(land.getPrposAreaCnflcAtNm()),
                zoneCodes.isBlank() ? "none" : zoneCodes, etcCodes.isBlank() ? "none" : etcCodes,
                slopeStr, solarStr, roadInfo,
                sunlightScore, areaScore,
                regulationScore != null ? regulationScore : "disqualified",
                zoneScore != null ? zoneScore : "disqualified",
                criteriaSection, roadScoreField);
    }

    private AiScoreResult parseAiResponse(String response, int sunlightScore, Integer roadScoreFromCode) {
        log.info("AI 분석 원본 응답:\n{}", response);
        if (response == null || response.isBlank()) {
            return new AiScoreResult(sunlightScore, 13, roadScoreFromCode != null ? roadScoreFromCode : 6, "AI 분석을 수행할 수 없습니다.");
        }

        Integer slope = 13;
        Integer roadScore = roadScoreFromCode; // 코드로 이미 계산된 경우 우선 사용

        Map<String, String> lines = new HashMap<>();
        for (String line : response.split("\n")) {
            int idx = line.indexOf(": ");
            if (idx > 0) lines.put(line.substring(0, idx).trim(), line.substring(idx + 2).trim());
        }
        try { String v = lines.getOrDefault("SLOPE_SCORE", "13"); slope = "null".equalsIgnoreCase(v) ? null : Integer.parseInt(v); } catch (Exception ignored) {}
        // road_side_code가 없을 때만 AI 값 사용
        if (roadScoreFromCode == null) {
            try { String v = lines.getOrDefault("ROAD_SCORE", "6"); roadScore = "null".equalsIgnoreCase(v) ? null : Integer.parseInt(v); } catch (Exception ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        appendRationale(sb, "일사량", lines.get("RATIONALE_SUNLIGHT"));
        appendRationale(sb, "경사도", lines.get("RATIONALE_SLOPE"));
        appendRationale(sb, "개발가능면적", lines.get("RATIONALE_AREA"));
        appendRationale(sb, "도로접근성", lines.get("RATIONALE_ROAD"));
        appendRationale(sb, "토지이용규제", lines.get("RATIONALE_REGULATION"));
        appendRationale(sb, "용도지역·용도지구", lines.get("RATIONALE_ZONE"));

        String rationale = sb.toString().trim();
        if (rationale.isBlank()) rationale = response.trim();
        return new AiScoreResult(sunlightScore, slope, roadScore, rationale);
    }

    private void appendRationale(StringBuilder sb, String label, String text) {
        if (text != null && !text.isBlank()) sb.append("[").append(label).append("] ").append(text).append("\n");
    }

    private record AiScoreResult(int sunlightScore, Integer slopeScore, Integer roadScore, String rationale) {}

    // ═══════════════════════════════════════════════════════════
    // 2. 연도별 공시지가 추이
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public LandPriceHistoryResponse getLandPriceHistory(Long landId, int years) {
        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
        if (land.getPnu() == null || land.getPnu().isBlank()) throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);

        PossessionAttrResponse apiResponse = vWorldService.getPossessionAttr(land.getPnu(), 1000);
        List<YearlyPrice> priceHistory = extractYearlyPrices(apiResponse, years, land.getArea());
        double avgGrowthRate = calcAvgGrowthRate(priceHistory);
        int fromYear = priceHistory.isEmpty() ? 0 : priceHistory.get(0).getYear();
        int toYear   = priceHistory.isEmpty() ? 0 : priceHistory.get(priceHistory.size() - 1).getYear();

        return LandPriceHistoryResponse.builder()
                .landId(land.getId()).address(land.getAddress()).area(land.getArea())
                .landCategory(land.getLcCodeNm()).priceHistory(priceHistory)
                .avgAnnualGrowthRate(avgGrowthRate).fromYear(fromYear).toYear(toYear).build();
    }

    private List<YearlyPrice> extractYearlyPrices(PossessionAttrResponse response, int years, Double area) {
        if (response == null || response.getPossessionAttr() == null || response.getPossessionAttr().getItem() == null) return Collections.emptyList();
        int currentYear = LocalDate.now().getYear(), fromYear = currentYear - years + 1;
        Map<Integer, PossessionAttrResponse.Item> yearMap = new TreeMap<>();
        for (PossessionAttrResponse.Item item : response.getPossessionAttr().getItem()) {
            if (item.getStdrYm() == null) continue;
            try {
                int year = Integer.parseInt(item.getStdrYm().substring(0, 4));
                if (year < fromYear || year > currentYear) continue;
                yearMap.merge(year, item, (e, n) -> n.getStdrYm().compareTo(e.getStdrYm()) >= 0 ? n : e);
            } catch (Exception e) { log.warn("stdrYm 파싱 실패: {}", item.getStdrYm()); }
        }
        List<YearlyPrice> result = new ArrayList<>();
        Long prevPrice = null;
        for (Map.Entry<Integer, PossessionAttrResponse.Item> entry : yearMap.entrySet()) {
            long price = parseLong(entry.getValue().getPblntfPclnd());
            Long total = (area != null && price > 0) ? (long)(price * area) : null;
            Double changeRate = (prevPrice != null && prevPrice > 0) ? Math.round(((double)(price - prevPrice) / prevPrice) * 1000.0) / 10.0 : null;
            result.add(YearlyPrice.builder().year(entry.getKey()).pricePerSqm(price > 0 ? price : null).totalPrice(total).changeRate(changeRate).build());
            prevPrice = price > 0 ? price : prevPrice;
        }
        return result;
    }

    private double calcAvgGrowthRate(List<YearlyPrice> history) {
        if (history.size() < 2) return 0.0;
        double sum = history.stream().mapToDouble(p -> p.getChangeRate() != null ? p.getChangeRate() : 0.0).sum();
        long count = history.stream().filter(p -> p.getChangeRate() != null).count();
        return count == 0 ? 0.0 : Math.round((sum / count) * 10.0) / 10.0;
    }

    private String toGrade(int score) {
        if (score >= 90) return "매우 좋음";
        if (score >= 80) return "좋음";
        if (score >= 70) return "보통";
        return "낮음";
    }

    private Land getLandWithDetails(Long landId) {
        Land land = landRepository.findWithZonesById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
        landRepository.findWithEtcsById(landId).ifPresent(l -> land.getLandEtcs().addAll(l.getLandEtcs()));
        return land;
    }

    private String nullSafe(String v) { return v != null ? v : "-"; }
    private long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim().replace(",", "")); } catch (NumberFormatException e) { return 0L; }
    }
}
