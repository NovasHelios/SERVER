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

        // 도로 점수는 AI를 호출하지 않고 도로접면코드로만 계산한다.
        Integer roadScore = calcRoadScore(land.getRoadSideCode());
        Integer slopeScore = calcSlopeScore(land.getAverageSlope());

        // 캐시 확인: 도로 점수도 null을 포함해 현재 계산 결과와 정확히 비교한다.
        if (land.getDevelopmentScoreCache() != null) {
            try {
                DevelopmentScoreResponse cached = objectMapper.readValue(
                        land.getDevelopmentScoreCache(), DevelopmentScoreResponse.class);
                if (Objects.equals(slopeScore, cached.getSlopeScore())
                        && Objects.equals(roadScore, cached.getRoadScore())
                        && Objects.equals(land.getRoadSideCode(), cached.getRoadSideCode())
                        && hasExpandedRationales(cached.getRationale())) {
                    return cached;
                }
                land.updateDevelopmentScoreCache(null);
            } catch (Exception e) {
                log.warn("개발가능성 캐시 역직렬화 실패, 재분석: {}", e.getMessage());
                land.updateDevelopmentScoreCache(null);
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

        // 점수는 서버에서 계산하고, AI는 제공된 점수의 평가 근거만 작성한다.
        AiScoreResult aiResult = callAiForScores(
                land, slopeScore, areaScore, roadScore, regulationScore, zoneScore,
                etcNamesStr, zoneNamesStr);

        boolean disqualified = slopeScore == null || roadScore == null
                || regulationScore == null || zoneScore == null;

        Integer totalScore = null;
        String grade;
        if (!disqualified) {
            totalScore = aiResult.sunlightScore() + slopeScore + areaScore
                    + roadScore + regulationScore + zoneScore;
            grade = toGrade(totalScore);
        } else {
            grade = "부적합";
        }

        DevelopmentScoreResponse result = DevelopmentScoreResponse.builder()
                .landId(land.getId())
                .address(land.getAddress())
                .sunlightScore(aiResult.sunlightScore())
                .slopeScore(slopeScore)
                .areaScore(areaScore)
                .roadScore(roadScore)
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
                .rationale(aiResult.rationale())
                .build();

        // 분석 결과 캐시 저장
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

    // ── 경사도 점수 (20점 만점, 평균 경사도 기반) ───────────────
    private Integer calcSlopeScore(Double averageSlope) {
        if (averageSlope == null || Double.isNaN(averageSlope)) return null;
        if (averageSlope <= 3) return 20;
        if (averageSlope <= 6) return 17;
        if (averageSlope <= 9) return 13;
        if (averageSlope <= 12) return 8;
        if (averageSlope <= 15) return 3;
        return null;
    }

    // ── 도로접근성 점수 (10점 만점, road_side_code 기반) ──
    // 01~05: 광대/중로 계열, 06~07: 소로 계열,
    // 08~09: 세로(가) 계열, 10~11: 세로(불) 계열, 12: 맹지
    private Integer calcRoadScore(String roadSideCode) {
        if (roadSideCode == null || roadSideCode.isBlank()) {
            return null; // 코드가 없으면 미확인으로 처리
        }

        String code = roadSideCode.trim();
        // 숫자 코드가 "1"처럼 저장되거나 "01"처럼 저장되는 경우를 통일한다.
        try {
            code = String.format("%02d", Integer.parseInt(code));
        } catch (NumberFormatException e) {
            return null;
        }

        return switch (code) {
            case "01", "02", "03", "04", "05" -> 10;
            case "06", "07" -> 8;
            case "08", "09" -> 6;
            case "10", "11" -> 3;
            case "12" -> null; // 맹지: 부적합
            default -> null;
        };
    }

    private AiScoreResult callAiForScores(Land land, Integer slopeScore, int areaScore,
                                          Integer roadScore, Integer regulationScore,
                                          Integer zoneScore, String etcCodes, String zoneCodes) {
        int sunlightScore = calcSunlightScore(land.getAverageSolarIrradiance());
        String prompt = buildAiPrompt(land, slopeScore, areaScore, roadScore,
                regulationScore, zoneScore, etcCodes, zoneCodes, sunlightScore);
        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .system("You are a professional land suitability analyst. Always respond strictly in the requested format.")
                    .user(prompt)
                    .call()
                    .content();
            return parseAiResponse(response, sunlightScore);
        } catch (Exception e) {
            log.warn("AI 분석 호출 실패: {}", e.getMessage());
            return new AiScoreResult(sunlightScore, buildFallbackRationale(land, slopeScore, roadScore));
        }
    }

    private String buildAiPrompt(Land land, Integer slopeScore, int areaScore, Integer roadScore,
                                 Integer regulationScore, Integer zoneScore, String etcCodes,
                                 String zoneCodes, int sunlightScore) {
        String slopeStr = land.getAverageSlope() != null
                ? String.format("%.2f", land.getAverageSlope())
                : "N/A";
        String solarStr = land.getAverageSolarIrradiance() != null
                ? String.format("%.3f kWh/m²/day", land.getAverageSolarIrradiance()) : "N/A";

        return String.format("""
                You are a land suitability analyst for solar panel installation projects in South Korea.
                Do not calculate or change any score. Write only evaluation rationales using the supplied data.
                Every rationale must be 2 to 3 short Korean sentences on separate lines.

                [Land Information]
                Address: %s / Land Category: %s / Actual Area: %.1f sqm / Desired Area: %s sqm
                Region: %s %s / Zoning: %s / Zoning Conflict: %s
                District Zones: %s / Other Regulations: %s
                Average Slope: %s degrees / Average Solar Irradiance: %s

                [Pre-calculated Scores: do not recalculate these]
                Solar Irradiance Score: %d / 25 / Slope Score: %s / 20 / Developable Area Score: %d / 10
                Road Accessibility Score: %s / 10 (Road Side Code: %s)
                Land Use Regulation Score: %s / 20 / Zoning Score: %s / 15

                Respond ONLY in this exact format:
                RATIONALE_SUNLIGHT: <2 or 3 sentences in Korean>
                RATIONALE_SLOPE: <2 or 3 sentences in Korean>
                RATIONALE_AREA: <2 or 3 sentences in Korean>
                RATIONALE_ROAD: <2 or 3 sentences in Korean>
                RATIONALE_REGULATION: <2 or 3 sentences in Korean>
                RATIONALE_ZONE: <2 or 3 sentences in Korean>
                
                Please write them in detail.
                """,
                nullSafe(land.getAddress()), nullSafe(land.getLcCodeNm()),
                land.getArea() != null ? land.getArea() : 0.0,
                land.getDesiredArea() != null ? String.valueOf(land.getDesiredArea()) : "N/A",
                nullSafe(land.getRegionSido()), nullSafe(land.getRegionSigungu()),
                nullSafe(land.getPrposAreaName()), nullSafe(land.getPrposAreaCnflcAtNm()),
                zoneCodes.isBlank() ? "none" : zoneCodes,
                etcCodes.isBlank() ? "none" : etcCodes,
                slopeStr, solarStr, sunlightScore,
                scoreText(slopeScore), areaScore, scoreText(roadScore), nullSafe(land.getRoadSideCode()),
                regulationScore != null ? regulationScore : "disqualified",
                zoneScore != null ? zoneScore : "disqualified");
    }

    private AiScoreResult parseAiResponse(String response, int sunlightScore) {
        log.info("AI 분석 원본 응답:\n{}", response);
        if (response == null || response.isBlank()) {
            return new AiScoreResult(sunlightScore, "AI 분석을 수행할 수 없습니다.");
        }

        Map<String, StringBuilder> rationales = new LinkedHashMap<>();
        String currentKey = null;
        for (String line : response.split("\\R")) {
            int idx = line.indexOf(':');
            String key = idx > 0 ? line.substring(0, idx).trim() : "";
            if (key.startsWith("RATIONALE_") && idx > 0) {
                currentKey = key;
                rationales.putIfAbsent(key, new StringBuilder());
                appendLine(rationales.get(key), line.substring(idx + 1).trim());
            } else if (currentKey != null && !line.isBlank()) {
                appendLine(rationales.get(currentKey), line.trim());
            }
        }

        StringBuilder sb = new StringBuilder();
        appendRationale(sb, "일사량", rationaleText(rationales, "RATIONALE_SUNLIGHT"));
        appendRationale(sb, "경사도", rationaleText(rationales, "RATIONALE_SLOPE"));
        appendRationale(sb, "개발가능면적", rationaleText(rationales, "RATIONALE_AREA"));
        appendRationale(sb, "도로접근성", rationaleText(rationales, "RATIONALE_ROAD"));
        appendRationale(sb, "토지이용규제", rationaleText(rationales, "RATIONALE_REGULATION"));
        appendRationale(sb, "용도지역·용도지구", rationaleText(rationales, "RATIONALE_ZONE"));

        String rationale = sb.toString().trim();
        if (rationale.isBlank()) {
            rationale = "AI 분석 결과를 해석할 수 없습니다.";
        }
        return new AiScoreResult(sunlightScore, rationale);
    }

    private void appendLine(StringBuilder sb, String text) {
        if (text == null || text.isBlank()) return;
        if (!sb.isEmpty()) sb.append("\n");
        sb.append(text.replaceFirst("^[-•]\\s*", ""));
    }

    private String rationaleText(Map<String, StringBuilder> rationales, String key) {
        StringBuilder text = rationales.get(key);
        return text == null ? null : text.toString();
    }

    private void appendRationale(StringBuilder sb, String label, String text) {
        if (text != null && !text.isBlank()) {
            sb.append("[").append(label).append("] ").append(text).append("\n");
        }
    }

    private String buildFallbackRationale(Land land, Integer slopeScore, Integer roadScore) {
        return "[일사량] AI 평가를 생성하지 못했습니다.\n제공된 일사량 점수를 기준으로 현장 일사량을 추가 확인하세요.\n"
                + "[경사도] 평균 경사도 "
                + (land.getAverageSlope() == null ? "정보가 없어" : String.format("%.2f°로", land.getAverageSlope()))
                + " 점수는 " + scoreText(slopeScore) + "점입니다.\n경사도 측정값과 지형을 현장에서 다시 확인하세요.\n"
                + "[개발가능면적] AI 평가를 생성하지 못했습니다.\n실제 설치 가능 면적은 현장 여건을 확인하세요.\n"
                + "[도로접근성] 도로접면코드 " + nullSafe(land.getRoadSideCode())
                + " 기준 점수는 " + scoreText(roadScore) + "점입니다.\n장비 반입과 진입 동선은 현장에서 확인하세요.\n"
                + "[토지이용규제] AI 평가를 생성하지 못했습니다.\n관련 인허가 조건을 관할 기관에 확인하세요.\n"
                + "[용도지역·용도지구] AI 평가를 생성하지 못했습니다.\n용도지역별 개발 가능 여부를 최종 확인하세요.";
    }

    private boolean hasExpandedRationales(String rationale) {
        if (rationale == null) return false;
        return List.of("[일사량]", "[경사도]", "[개발가능면적]", "[도로접근성]", "[토지이용규제]", "[용도지역·용도지구]")
                .stream().allMatch(rationale::contains)
                && rationale.lines().count() >= 12;
    }

    private String scoreText(Integer score) { return score == null ? "부적합" : String.valueOf(score); }

    private record AiScoreResult(int sunlightScore, String rationale) {}

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
