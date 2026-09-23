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

    // ── 가중치 (합산 = 100) ─────────────────────────────────────
    private static final double W_SUNLIGHT      = 0.40;
    private static final double W_SLOPE         = 0.30;
    private static final double W_ACCESSIBILITY = 0.30;

    // ── 공시지가 기본 조회 연수 ─────────────────────────────────
    private static final int DEFAULT_YEARS = 6;

    // ═══════════════════════════════════════════════════════════
    // 1. 개발가능성 분석
    // ═══════════════════════════════════════════════════════════

    /**
     * 토지 ID로 개발가능성 백분위 점수를 산출합니다.
     *
     * <p>점수 산출 기준:</p>
     * <ul>
     *   <li>일조량 (40%): 지목코드·용도지역 기반 일조 적합성 평가</li>
     *   <li>면적/경사 (30%): 면적 규모·지목 기반 경사도 적합성 평가</li>
     *   <li>접근성 (30%): 행정구역(시도·시군구)·지목·용도지역 기반 접근성 평가</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public DevelopmentScoreResponse analyzeDevelopment(Long landId) {
        Land land = getLandWithDetails(landId);

        int sunlight      = calcSunlightScore(land);
        int slope         = calcSlopeScore(land);
        int accessibility = calcAccessibilityScore(land);

        int total = (int) Math.round(
                sunlight      * W_SUNLIGHT
                + slope         * W_SLOPE
                + accessibility * W_ACCESSIBILITY
        );
        total = clamp(total, 0, 100);

        // 용도지구 코드 목록
        String zoneCodes = land.getLandZones().stream()
                .map(LandZone::getCode)
                .collect(Collectors.joining(","));

        // 기타 규제 코드 목록
        String etcCodes = land.getLandEtcs().stream()
                .map(LandEtc::getCode)
                .collect(Collectors.joining(","));

        return DevelopmentScoreResponse.builder()
                .landId(land.getId())
                .address(land.getAddress())
                .sunlightScore(sunlight)
                .slopeScore(slope)
                .accessibilityScore(accessibility)
                .developmentScore(total)
                .grade(toGrade(total))
                .landCategory(land.getLcCodeNm())
                .area(land.getArea())
                .zoneConflict(land.getPrposAreaCnflcAtNm())
                .zoneCodes(zoneCodes.isBlank() ? null : zoneCodes)
                .etcCodes(etcCodes.isBlank() ? null : etcCodes)
                .rationale(buildRationale(land, sunlight, slope, accessibility, total))
                .build();
    }

    // ── 일조량 점수 (0~100) ─────────────────────────────────────
    /**
     * 지목코드(lcCode)·용도지역 저촉여부·기타규제 코드를 기반으로 일조 적합성을 평가합니다.
     *
     * <ul>
     *   <li>지목: 대지·전·답·과수원은 개방형 지형 → 일조 우수</li>
     *   <li>지목: 임야(06)·하천(10)·구거(11)는 그늘·음지 가능성 높음 → 감점</li>
     *   <li>저촉여부: 저촉(2)이면 개발제한 가능성 → 감점</li>
     *   <li>기타규제: 보전임지(FA), 공원(UQP), 군사(GF) 등 → 감점</li>
     * </ul>
     */
    private int calcSunlightScore(Land land) {
        int score = 60; // 기본점수

        // ① 지목 기반 조정
        String lc = land.getLcCode();
        if (lc != null) {
            score += switch (lc) {
                case "08" -> 20;          // 대지 — 평탄, 일조 최우수
                case "01", "02" -> 15;    // 전·답 — 평탄 농지, 일조 우수
                case "04" -> 10;          // 과수원 — 비교적 양호
                case "07" -> 5;           // 목장용지
                case "06" -> -15;         // 임야 — 경사·수목으로 일조 불리
                case "10", "11" -> -20;   // 하천·구거 — 저지대, 일조 불리
                case "16" -> -10;         // 도로
                default -> 0;
            };
        }

        // ② 용도지역 저촉여부 조정
        String cnflc = land.getPrposAreaCnflcAt();
        if ("2".equals(cnflc)) score -= 10; // 저촉

        // ③ 기타 규제 조정
        Set<String> etcCodes = land.getLandEtcs().stream()
                .map(LandEtc::getCode).collect(Collectors.toSet());
        if (etcCodes.stream().anyMatch(c -> c.startsWith("FA"))) score -= 15; // 보전임지
        if (etcCodes.stream().anyMatch(c -> c.startsWith("GF"))) score -= 10; // 군사시설
        if (etcCodes.stream().anyMatch(c -> c.startsWith("UQP"))) score -= 8; // 공원

        return clamp(score, 0, 100);
    }

    // ── 면적/경사 점수 (0~100) ──────────────────────────────────
    /**
     * 면적 규모·지목을 기반으로 경사도 적합성을 평가합니다.
     *
     * <ul>
     *   <li>대지: 이미 평탄화된 경우가 많음 → 높은 기본점수</li>
     *   <li>농지(전·답): 농업용 평탄 지형 → 양호</li>
     *   <li>임야: 경사 지형 가능성 높음 → 감점</li>
     *   <li>면적: 330㎡ 미만(소규모)이면 개발 한계, 3000㎡ 이상이면 가산</li>
     * </ul>
     */
    private int calcSlopeScore(Land land) {
        int score = 55; // 기본점수

        // ① 지목 기반 조정
        String lc = land.getLcCode();
        if (lc != null) {
            score += switch (lc) {
                case "08" -> 25;          // 대지 — 평탄화 완료
                case "01", "02" -> 20;    // 전·답 — 농업용 평탄 지형
                case "04" -> 10;          // 과수원
                case "07" -> 5;           // 목장용지 — 완만한 경사
                case "06" -> -20;         // 임야 — 경사 지형
                case "09" -> -5;          // 광천지
                default -> 0;
            };
        }

        // ② 면적 기반 조정
        if (land.getArea() != null) {
            double area = land.getArea();
            if (area < 100)        score -= 20; // 극소규모
            else if (area < 330)   score -= 10; // 소규모 (33평 미만)
            else if (area < 1000)  score += 0;  // 일반
            else if (area < 3000)  score += 5;  // 중규모
            else if (area < 10000) score += 10; // 대규모
            else                   score += 15; // 초대형
        }

        // ③ 기타 규제 조정
        Set<String> etcCodes = land.getLandEtcs().stream()
                .map(LandEtc::getCode).collect(Collectors.toSet());
        if (etcCodes.stream().anyMatch(c -> c.startsWith("FA"))) score -= 15;
        if (etcCodes.stream().anyMatch(c -> c.startsWith("HA"))) score -= 10; // 급경사지

        return clamp(score, 0, 100);
    }

    // ── 접근성 점수 (0~100) ─────────────────────────────────────
    /**
     * 행정구역(시도·시군구)·지목·용도지역·기타규제 기반으로 접근성을 평가합니다.
     *
     * <ul>
     *   <li>서울·인천·경기: 도로망 밀도 높음 → 높은 기본점수</li>
     *   <li>지목 대지(08): 이미 진입로 확보 가능성 높음</li>
     *   <li>임야·하천: 진입로 확보 어려움 → 감점</li>
     *   <li>군사·보전구역: 접근 제한 → 큰 감점</li>
     *   <li>용도지역 저촉: 접근 제한 가능성</li>
     * </ul>
     */
    private int calcAccessibilityScore(Land land) {
        int score = 50; // 기본점수

        // ① 시도 기반 조정 (도로망 밀도)
        String sido = land.getRegionSido();
        if (sido != null) {
            score += switch (sido) {
                case "서울특별시"            -> 30;
                case "인천광역시",
                     "경기도"              -> 25;
                case "부산광역시",
                     "대구광역시",
                     "광주광역시",
                     "대전광역시",
                     "울산광역시"           -> 20;
                case "세종특별자치시"         -> 18;
                case "충청북도", "충청남도",
                     "전라북도", "경상북도",
                     "경상남도"            -> 5;
                case "전라남도", "강원특별자치도",
                     "제주특별자치도"         -> 0;
                default                   -> 5;
            };
        }

        // ② 지목 기반 조정
        String lc = land.getLcCode();
        if (lc != null) {
            score += switch (lc) {
                case "08" -> 10;          // 대지 — 진입로 확보 가능성 높음
                case "16" -> 15;          // 도로 — 접근 용이
                case "01", "02" -> 5;     // 전·답 — 농로 존재 가능
                case "06" -> -15;         // 임야 — 진입로 확보 어려움
                case "10", "11" -> -15;   // 하천·구거 — 접근 제한
                default -> 0;
            };
        }

        // ③ 용도지역 저촉여부 조정
        String cnflc = land.getPrposAreaCnflcAt();
        if ("2".equals(cnflc)) score -= 10;

        // ④ 기타 규제 조정
        Set<String> etcCodes = land.getLandEtcs().stream()
                .map(LandEtc::getCode).collect(Collectors.toSet());
        if (etcCodes.stream().anyMatch(c -> c.startsWith("GF"))) score -= 20; // 군사시설
        if (etcCodes.stream().anyMatch(c -> c.startsWith("FA"))) score -= 10; // 보전임지
        if (etcCodes.stream().anyMatch(c -> c.startsWith("DA"))) score += 10; // 도시개발구역

        return clamp(score, 0, 100);
    }

    // ═══════════════════════════════════════════════════════════
    // 2. 연도별 공시지가 추이
    // ═══════════════════════════════════════════════════════════

    /**
     * 토지 ID로 연도별 공시지가 추이를 조회합니다.
     *
     * <p>VWorld getPossessionAttr API에서 stdrYm(기준연월) 필드를 기준으로
     * 연도별 최신 항목 하나씩 추출하여 추이를 구성합니다.</p>
     */
    @Transactional(readOnly = true)
    public LandPriceHistoryResponse getLandPriceHistory(Long landId, int years) {
        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (land.getPnu() == null || land.getPnu().isBlank()) {
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }

        // VWorld API 호출 (numOfRows=1000으로 여러 연도치 수집)
        PossessionAttrResponse apiResponse = vWorldService.getPossessionAttr(land.getPnu(), 1000);

        List<YearlyPrice> priceHistory = extractYearlyPrices(apiResponse, years, land.getArea());

        // 평균 연간 상승률 계산
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

    /**
     * API 응답에서 연도별 대표 공시지가를 추출합니다.
     * stdrYm(기준연월) 기준으로 연도별 가장 최신 항목 1개씩 선택합니다.
     */
    private List<YearlyPrice> extractYearlyPrices(
            PossessionAttrResponse response, int years, Double area) {

        if (response == null
                || response.getPossessionAttr() == null
                || response.getPossessionAttr().getItem() == null) {
            return Collections.emptyList();
        }

        int currentYear = LocalDate.now().getYear();
        int fromYear    = currentYear - years + 1;

        // stdrYm(예: "2023-01") 기준 연도별 가장 최신 항목 수집
        Map<Integer, PossessionAttrResponse.Item> yearMap = new TreeMap<>();
        for (PossessionAttrResponse.Item item : response.getPossessionAttr().getItem()) {
            if (item.getStdrYm() == null) continue;
            try {
                int year = Integer.parseInt(item.getStdrYm().substring(0, 4));
                if (year < fromYear || year > currentYear) continue;
                // 같은 연도면 더 최신(stdrYm 큰 값) 항목으로 교체
                yearMap.merge(year, item, (existing, newItem) ->
                        newItem.getStdrYm().compareTo(existing.getStdrYm()) >= 0 ? newItem : existing
                );
            } catch (Exception e) {
                log.warn("stdrYm 파싱 실패: {}", item.getStdrYm());
            }
        }

        // YearlyPrice 목록 생성 + 전년 대비 변동률 계산
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

    // ── 평균 연간 상승률 계산 ────────────────────────────────────
    private double calcAvgGrowthRate(List<YearlyPrice> history) {
        if (history.size() < 2) return 0.0;
        double sum = history.stream()
                .mapToDouble(p -> p.getChangeRate() != null ? p.getChangeRate() : 0.0)
                .sum();
        long count = history.stream().filter(p -> p.getChangeRate() != null).count();
        if (count == 0) return 0.0;
        return Math.round((sum / count) * 10.0) / 10.0;
    }

    // ── 등급 변환 ────────────────────────────────────────────────
    private String toGrade(int score) {
        if (score >= 80) return "매우높음";
        if (score >= 65) return "높음";
        if (score >= 45) return "보통";
        if (score >= 30) return "낮음";
        return "매우낮음";
    }

    // ── 점수 근거 문자열 생성 ─────────────────────────────────────
    private String buildRationale(Land land, int sunlight, int slope, int access, int total) {
        return String.format(
                "지목[%s] 면적[%.1f㎡] 위치[%s %s] | 일조량:%d점(40%%) + 경사:%d점(30%%) + 접근성:%d점(30%%) = 종합 %d점(%s)",
                nullSafe(land.getLcCodeNm()),
                land.getArea() != null ? land.getArea() : 0.0,
                nullSafe(land.getRegionSido()),
                nullSafe(land.getRegionSigungu()),
                sunlight, slope, access,
                total, toGrade(total)
        );
    }

    // ── 유틸 ─────────────────────────────────────────────────────
    private Land getLandWithDetails(Long landId) {
        // zones 와 etcs를 각각 fetch join (MultipleBagFetchException 방지)
        Land land = landRepository.findWithZonesById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
        // etcs는 별도 쿼리로 로드
        landRepository.findWithEtcsById(landId).ifPresent(l ->
                land.getLandEtcs().addAll(l.getLandEtcs())
        );
        return land;
    }

    private String nullSafe(String v) { return v != null ? v : "-"; }

    private long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim().replace(",", "")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
