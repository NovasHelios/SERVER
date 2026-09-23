package com.heilous.analysis.service;

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

    private static final int DEFAULT_YEARS = 6;

    // ═══════════════════════════════════════════════════════════
    // 1. 개발가능성 분석
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public DevelopmentScoreResponse analyzeDevelopment(Long landId) {
        Land land = getLandWithDetails(landId);

        String etcCodesStr = land.getLandEtcs().stream()
                .map(LandEtc::getCode).collect(Collectors.joining(","));
        String zoneCodesStr = land.getLandZones().stream()
                .map(LandZone::getCode).collect(Collectors.joining(","));

        // ── 항목별 점수 산출 ─────────────────────────────────────
        // 일사량, 경사도, 도로접근성은 AI가 판단 (데이터 없음)
        // → AI 호출 시 함께 받아오므로 아래에서 처리

        // 개발 가능 면적: desiredArea 우선, 없으면 area
        Double evalArea = land.getDesiredArea() != null ? land.getDesiredArea() : land.getArea();
        int areaScore = calcAreaScore(evalArea);

        // 토지이용규제: etcCodes 기반 (null = 데이터 없어 AI 판단)
        Integer regulationScore = calcRegulationScore(etcCodesStr);

        // 용도지역·용도지구: prposAreaCode + zoneCodes 기반 (null = 부적합)
        Integer zoneScore = calcZoneScore(land.getPrposAreaCode(), zoneCodesStr);

        // ── AI로 일사량·경사도·도로접근성 점수 + 전체 rationale 생성 ──
        AiScoreResult aiResult = callAiForScores(land, areaScore, regulationScore, zoneScore,
                etcCodesStr, zoneCodesStr);

        // ── 부적합 판정 ──────────────────────────────────────────
        boolean disqualified = aiResult.slopeScore == null
                || aiResult.roadScore == null
                || regulationScore == null
                || zoneScore == null;

        // 총점 계산
        Integer totalScore = null;
        String grade;
        if (!disqualified) {
            totalScore = aiResult.sunlightScore + aiResult.slopeScore + areaScore
                    + aiResult.roadScore + regulationScore + zoneScore;
            grade = toGrade(totalScore);
        } else {
            grade = "부적합";
        }

        return DevelopmentScoreResponse.builder()
                .landId(land.getId())
                .address(land.getAddress())
                .sunlightScore(aiResult.sunlightScore)
                .slopeScore(aiResult.slopeScore)
                .areaScore(areaScore)
                .roadScore(aiResult.roadScore)
                .regulationScore(regulationScore)
                .zoneScore(zoneScore)
                .totalScore(totalScore)
                .grade(grade)
                .disqualified(disqualified)
                .landCategory(land.getLcCodeNm())
                .area(land.getArea())
                .desiredArea(land.getDesiredArea())
                .prposAreaCode(land.getPrposAreaCode())
                .zoneConflict(land.getPrposAreaCnflcAtNm())
                .zoneCodes(zoneCodesStr.isBlank() ? null : zoneCodesStr)
                .etcCodes(etcCodesStr.isBlank() ? null : etcCodesStr)
                .rationale(aiResult.rationale)
                .build();
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

    // ── 토지이용규제 점수 (20점 만점, 태양광 막는 규제 시 null=부적합) ──
    private Integer calcRegulationScore(String etcCodesStr) {
        if (etcCodesStr == null || etcCodesStr.isBlank()) return 20; // 규제 없음

        Set<String> codes = Arrays.stream(etcCodesStr.split(","))
                .map(String::trim).collect(Collectors.toSet());

        // 태양광 개발을 원천 차단하는 규제 → 부적합
        boolean disqualify = codes.stream().anyMatch(c ->
                c.startsWith("UBD")   // 개발제한구역
                || c.startsWith("FA")  // 보전산지
                || c.startsWith("GF")  // 군사시설보호구역
                || c.startsWith("UBB") // 절대보전지역
        );
        if (disqualify) return null;

        // 복수의 검토사항 (3개 이상)
        if (codes.size() >= 3) return 7;
        // 경미한 검토사항 (1~2개)
        if (!codes.isEmpty()) return 14;
        return 20;
    }

    // ── 용도지역·용도지구 점수 (15점 만점) ──────────────────────
    private Integer calcZoneScore(String prposAreaCode, String zoneCodesStr) {
        // 개발 불가 용도지역 → 부적합
        if (prposAreaCode != null) {
            // UQA02X = 보전관리, UQA03X = 농림, UQA04X = 자연환경보전
            if (prposAreaCode.startsWith("UQA02") || prposAreaCode.startsWith("UQA03")
                    || prposAreaCode.startsWith("UQA04")) {
                return null; // 부적합
            }
            // 개발에 유리한 용도지역: 계획관리(UQA01), 생산관리 일부
            if (prposAreaCode.startsWith("UQA01")) return 15; // 계획관리지역
        }

        // 용도지구 코드 확인
        boolean hasRestrictiveZone = false;
        if (zoneCodesStr != null && !zoneCodesStr.isBlank()) {
            Set<String> zoneCodes = Arrays.stream(zoneCodesStr.split(","))
                    .map(String::trim).collect(Collectors.toSet());
            hasRestrictiveZone = zoneCodes.stream().anyMatch(c ->
                    c.startsWith("UQG") || c.startsWith("UQH") || c.startsWith("UQI"));
        }

        if (prposAreaCode == null && (zoneCodesStr == null || zoneCodesStr.isBlank())) return 8; // 추가 검토 필요
        if (hasRestrictiveZone) return 3;
        return 12; // 비교적 유리
    }

    // ── AI 호출: 일사량·경사도·도로접근성 점수 + rationale ───────
    private AiScoreResult callAiForScores(Land land, int areaScore, Integer regulationScore,
                                           Integer zoneScore, String etcCodes, String zoneCodes) {
        String prompt = buildAiPrompt(land, areaScore, regulationScore, zoneScore, etcCodes, zoneCodes);
        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .system("You are a professional land suitability analyst. Always respond strictly in the requested format.")
                    .user(prompt)
                    .call()
                    .content();
            return parseAiResponse(response);
        } catch (Exception e) {
            log.warn("AI 분석 호출 실패: {}", e.getMessage());
            return new AiScoreResult(18, 13, 6, "AI 분석을 수행할 수 없습니다.");
        }
    }

    private String buildAiPrompt(Land land, int areaScore, Integer regulationScore,
                                  Integer zoneScore, String etcCodes, String zoneCodes) {
        return String.format("""
                You are a land suitability analyst for solar panel installation projects in South Korea.
                Based on the land information below, evaluate the following 3 criteria and provide a brief one-line rationale for each of the 6 criteria listed.
                
                [Land Information]
                Address: %s
                Land Category: %s
                Actual Area: %.1f sqm
                Desired Area: %s sqm
                Region (Province): %s
                Region (City/County): %s
                Zoning Code: %s
                Zoning Conflict Status: %s
                District Codes: %s
                Other Regulation Codes: %s
                
                [Pre-calculated Scores]
                Developable Area Score: %d / 10
                Land Use Regulation Score: %s / 20
                Zoning Score: %s / 15
                
                [Scoring Criteria]
                1. Solar Irradiance (max 25 pts)
                   - Excellent: 25 / Good: 22 / Average: 18 / Low: 12 / Very Low: 5
                
                2. Slope (max 20 pts)
                   - 0~5 deg: 20 / 5~10 deg: 17 / 10~15 deg: 13 / 15~20 deg: 8 / 20~25 deg: 3 / over 25 deg: null (disqualified)
                
                3. Road Accessibility (max 10 pts)
                   - Direct road access: 10 / within 50m: 8 / 50~100m: 6 / 100~200m: 3 / over 200m: null (disqualified)
                
                Respond ONLY in the exact format below, no extra text:
                SUNLIGHT_SCORE: <number>
                SLOPE_SCORE: <number or null>
                ROAD_SCORE: <number or null>
                RATIONALE_SUNLIGHT: <one-line reason in Korean>
                RATIONALE_SLOPE: <one-line reason in Korean>
                RATIONALE_AREA: <one-line reason in Korean>
                RATIONALE_ROAD: <one-line reason in Korean>
                RATIONALE_REGULATION: <one-line reason in Korean>
                RATIONALE_ZONE: <one-line reason in Korean>
                """,
                nullSafe(land.getAddress()),
                nullSafe(land.getLcCodeNm()),
                land.getArea() != null ? land.getArea() : 0.0,
                land.getDesiredArea() != null ? String.valueOf(land.getDesiredArea()) : "N/A",
                nullSafe(land.getRegionSido()),
                nullSafe(land.getRegionSigungu()),
                nullSafe(land.getPrposAreaCode()),
                nullSafe(land.getPrposAreaCnflcAtNm()),
                zoneCodes.isBlank() ? "none" : zoneCodes,
                etcCodes.isBlank() ? "none" : etcCodes,
                areaScore,
                regulationScore != null ? regulationScore : "disqualified",
                zoneScore != null ? zoneScore : "disqualified"
        );
    }

    private AiScoreResult parseAiResponse(String response) {
        log.info("AI 분석 원본 응답:\n{}", response);

        if (response == null || response.isBlank()) {
            log.warn("AI 응답이 비어있음");
            return new AiScoreResult(18, 13, 6, "AI 분석을 수행할 수 없습니다.");
        }

        int sunlight = 18;
        Integer slope = 13, roadScore = 6;

        // 키: 값 파싱 — 첫 번째 ": " 만 구분자로 사용 (값 안에 콜론 있어도 안전)
        Map<String, String> lines = new HashMap<>();
        for (String line : response.split("\n")) {
            int idx = line.indexOf(": ");
            if (idx > 0) {
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 2).trim();
                lines.put(key, val);
            }
        }

        log.info("파싱된 AI 응답 키 목록: {}", lines.keySet());

        try { sunlight = Integer.parseInt(lines.getOrDefault("SUNLIGHT_SCORE", "18")); } catch (Exception ignored) {}
        try {
            String v = lines.getOrDefault("SLOPE_SCORE", "13");
            slope = "null".equalsIgnoreCase(v) ? null : Integer.parseInt(v);
        } catch (Exception ignored) {}
        try {
            String v = lines.getOrDefault("ROAD_SCORE", "6");
            roadScore = "null".equalsIgnoreCase(v) ? null : Integer.parseInt(v);
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        appendRationale(sb, "일사량", lines.get("RATIONALE_SUNLIGHT"));
        appendRationale(sb, "경사도", lines.get("RATIONALE_SLOPE"));
        appendRationale(sb, "개발가능면적", lines.get("RATIONALE_AREA"));
        appendRationale(sb, "도로접근성", lines.get("RATIONALE_ROAD"));
        appendRationale(sb, "토지이용규제", lines.get("RATIONALE_REGULATION"));
        appendRationale(sb, "용도지역·용도지구", lines.get("RATIONALE_ZONE"));

        String rationale = sb.toString().trim();
        if (rationale.isBlank()) {
            log.warn("rationale 조합 결과가 비어있음. 원본 응답을 그대로 사용");
            rationale = response.trim();
        }

        return new AiScoreResult(sunlight, slope, roadScore, rationale);
    }

    private void appendRationale(StringBuilder sb, String label, String text) {
        if (text != null && !text.isBlank()) {
            sb.append("[").append(label).append("] ").append(text).append("\n");
        }
    }

    private record AiScoreResult(int sunlightScore, Integer slopeScore, Integer roadScore, String rationale) {}

    // ═══════════════════════════════════════════════════════════
    // 2. 연도별 공시지가 추이
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public LandPriceHistoryResponse getLandPriceHistory(Long landId, int years) {
        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (land.getPnu() == null || land.getPnu().isBlank()) {
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }

        PossessionAttrResponse apiResponse = vWorldService.getPossessionAttr(land.getPnu(), 1000);
        List<YearlyPrice> priceHistory = extractYearlyPrices(apiResponse, years, land.getArea());
        double avgGrowthRate = calcAvgGrowthRate(priceHistory);

        int fromYear = priceHistory.isEmpty() ? 0 : priceHistory.get(0).getYear();
        int toYear   = priceHistory.isEmpty() ? 0 : priceHistory.get(priceHistory.size() - 1).getYear();

        return LandPriceHistoryResponse.builder()
                .landId(land.getId())
                .address(land.getAddress())
                .area(land.getArea())
                .landCategory(land.getLcCodeNm())
                .priceHistory(priceHistory)
                .avgAnnualGrowthRate(avgGrowthRate)
                .fromYear(fromYear)
                .toYear(toYear)
                .build();
    }

    private List<YearlyPrice> extractYearlyPrices(PossessionAttrResponse response, int years, Double area) {
        if (response == null
                || response.getPossessionAttr() == null
                || response.getPossessionAttr().getItem() == null) {
            return Collections.emptyList();
        }

        int currentYear = LocalDate.now().getYear();
        int fromYear    = currentYear - years + 1;

        Map<Integer, PossessionAttrResponse.Item> yearMap = new TreeMap<>();
        for (PossessionAttrResponse.Item item : response.getPossessionAttr().getItem()) {
            if (item.getStdrYm() == null) continue;
            try {
                int year = Integer.parseInt(item.getStdrYm().substring(0, 4));
                if (year < fromYear || year > currentYear) continue;
                yearMap.merge(year, item, (existing, newItem) ->
                        newItem.getStdrYm().compareTo(existing.getStdrYm()) >= 0 ? newItem : existing);
            } catch (Exception e) {
                log.warn("stdrYm 파싱 실패: {}", item.getStdrYm());
            }
        }

        List<YearlyPrice> result = new ArrayList<>();
        Long prevPrice = null;
        for (Map.Entry<Integer, PossessionAttrResponse.Item> entry : yearMap.entrySet()) {
            long price = parseLong(entry.getValue().getPblntfPclnd());
            Long total = (area != null && price > 0) ? (long) (price * area) : null;
            Double changeRate = null;
            if (prevPrice != null && prevPrice > 0) {
                changeRate = Math.round(((double)(price - prevPrice) / prevPrice) * 1000.0) / 10.0;
            }
            result.add(YearlyPrice.builder()
                    .year(entry.getKey())
                    .pricePerSqm(price > 0 ? price : null)
                    .totalPrice(total)
                    .changeRate(changeRate)
                    .build());
            prevPrice = price > 0 ? price : prevPrice;
        }
        return result;
    }

    private double calcAvgGrowthRate(List<YearlyPrice> history) {
        if (history.size() < 2) return 0.0;
        double sum = history.stream().mapToDouble(p -> p.getChangeRate() != null ? p.getChangeRate() : 0.0).sum();
        long count = history.stream().filter(p -> p.getChangeRate() != null).count();
        if (count == 0) return 0.0;
        return Math.round((sum / count) * 10.0) / 10.0;
    }

    // ── 등급 변환 ────────────────────────────────────────────────
    private String toGrade(int score) {
        if (score >= 90) return "매우 좋음";
        if (score >= 80) return "좋음";
        if (score >= 70) return "보통";
        return "낮음";
    }

    // ── 유틸 ─────────────────────────────────────────────────────
    private Land getLandWithDetails(Long landId) {
        Land land = landRepository.findWithZonesById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
        landRepository.findWithEtcsById(landId).ifPresent(l ->
                land.getLandEtcs().addAll(l.getLandEtcs()));
        return land;
    }

    private String nullSafe(String v) { return v != null ? v : "-"; }

    private long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim().replace(",", "")); }
        catch (NumberFormatException e) { return 0L; }
    }
}
