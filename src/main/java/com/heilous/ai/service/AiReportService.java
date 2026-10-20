package com.heilous.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.ai.dto.AiReportResponse;
import com.heilous.ai.dto.SolarCriterionReport;
import com.heilous.ai.dto.SolarSuitabilityReportResponse;
import com.heilous.ai.dto.SolarSummaryReportResponse;
import com.heilous.analysis.dto.DevelopmentScoreResponse;
import com.heilous.analysis.service.LandAnalysisService;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportService {

    private final ChatClient chatClient;
    private final LandRepository landRepository;
    private final LandAnalysisService landAnalysisService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiReportResponse generateReport(Long landId) {
        Land land = getLand(landId);

        // 캐시 히트
        if (land.getAiReportCache() != null) {
            return new AiReportResponse(land.getId(), land.getAddress(), land.getAiReportCache());
        }

        String report = chatClient.prompt().user(buildPrompt(land)).call().content();
        String result = report == null ? "" : report.trim();

        land.updateAiReportCache(result);
        return new AiReportResponse(land.getId(), land.getAddress(), result);
    }

    @Transactional
    public SolarSuitabilityReportResponse generateSolarSuitabilityReport(Long landId) {
        Land land = getLand(landId);

        // 캐시 히트
        if (land.getSolarSuitabilityCache() != null) {
            try {
                return objectMapper.readValue(land.getSolarSuitabilityCache(), SolarSuitabilityReportResponse.class);
            } catch (Exception e) {
                log.warn("태양광 적합도 캐시 역직렬화 실패, 재생성: {}", e.getMessage());
            }
        }

        // 개발가능성 분석 (캐시 우선 사용)
        DevelopmentScoreResponse scores = landAnalysisService.analyzeDevelopment(landId);

        List<Criterion> criteria = List.of(
                new Criterion("일사량", scores.getSunlightScore(), 25),
                new Criterion("경사도", scores.getSlopeScore(), 20),
                new Criterion("개발 가능 면적", scores.getAreaScore(), 10),
                new Criterion("도로 접근성", scores.getRoadScore(), 10),
                new Criterion("토지이용규제", scores.getRegulationScore(), 20),
                new Criterion("용도지역·용도지구", scores.getZoneScore(), 15));

        String response = chatClient.prompt().user(buildCriteriaPrompt(land, scores, criteria)).call().content();
        Map<String, String> explanations = parseKeyValues(response);

        List<SolarCriterionReport> reports = new ArrayList<>();
        for (int i = 0; i < criteria.size(); i++) {
            Criterion c = criteria.get(i);
            String explanation = explanations.get("CRITERION_" + (i + 1));
            if (explanation == null || explanation.isBlank()) {
                explanation = "현재 평가 점수 " + scoreText(c.score) + "을 기준으로 산정되었습니다. 현장 조건과 인허가 사항을 함께 확인해야 합니다.";
            }
            reports.add(new SolarCriterionReport(c.name, c.score, c.maxScore, explanation));
        }

        SolarSuitabilityReportResponse result = new SolarSuitabilityReportResponse(land.getId(), land.getAddress(), reports);

        // 캐시 저장
        try {
            land.updateSolarSuitabilityCache(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("태양광 적합도 캐시 저장 실패: {}", e.getMessage());
        }

        return result;
    }

    @Transactional
    public SolarSummaryReportResponse generateSolarSummaryReport(Long landId) {
        Land land = getLand(landId);

        // 캐시 히트
        if (land.getSolarSummaryCache() != null) {
            try {
                SolarSummaryReportResponse cached = objectMapper.readValue(
                        land.getSolarSummaryCache(), SolarSummaryReportResponse.class);
                if (hasSummarySections(cached)) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("태양광 요약 캐시 역직렬화 실패, 재생성: {}", e.getMessage());
            }
        }

        // 개발가능성 분석 (캐시 우선 사용)
        DevelopmentScoreResponse s = landAnalysisService.analyzeDevelopment(landId);

        String prompt = """
                당신은 한국 토지의 태양광 설치 후보 적합성을 설명하는 분석가입니다.
                제공한 토지 상세정보와 적합도 결과만 근거로 세 종류의 평가를 작성하세요.
                각 평가는 2~4문장을 한 줄로 작성하며, 문장은 공백으로 구분하세요.
                유리한 조건에는 실제로 유리한 점수·수치·용도지역명·규제명을 직접 언급하고 태양광 설치에 어떤 이점인지 설명하세요. 유리한 근거가 없다면 "확인된 유리한 조건 없음"이라고 작성하세요.
                제약 요인에는 점수가 낮은 항목, 부적합 사유, 규제·용도지역 제약을 빠짐없이 작성하세요. "안 좋은 점이 있습니다", "제약이 있을 수 있습니다"처럼 두루뭉술하게 쓰지 말고, 원인과 영향을 구체적으로 설명하세요.
                추가 확인 사항에는 현장 또는 인허가 단계에서 무엇을 누구에게 또는 어떤 방법으로 확인해야 하는지 구체적으로 작성하세요. "추가 확인이 필요합니다"로 끝내지 마세요.
                주어지지 않은 현장 사실(실제 경사도, 일사량, 도로까지 거리 등)은 추정해 사실처럼 쓰지 말고 확인이 필요하다고 명시하세요.
                부적합이면 그 사유를 제약 요인에 분명히 설명하고 가능성을 과장하지 마세요.

                아래 형식의 세 줄만 반환하세요. 키 이름을 바꾸거나 Markdown 제목·불릿·코드 블록을 추가하지 마세요.
                FAVORABLE_CONDITIONS: <유리한 조건 설명>
                CONSTRAINTS: <제약 요인 설명>
                ADDITIONAL_CHECKS: <추가 확인 사항 설명>

                [토지 상세정보]
                주소: %s / 지역: %s %s %s / 지목/면적: %s / %s㎡ (희망 면적: %s㎡)
                대장 구분/공유인수: %s / %s
                희망가격/거래유형: %s / %s
                소유자 설명: %s
                용도지역: %s (%s), 저촉여부: %s
                용도지구: %s / 기타 규제: %s
                [적합도 결과]
                일사량 %s/25, 경사도 %s/20, 개발 가능 면적 %s/10, 도로 접근성 %s/10,
                토지이용규제 %s/20, 용도지역·용도지구 %s/15
                총점: %s/100, 등급: %s, 부적합: %s
                항목별 산정 근거: %s
                """.formatted(nullSafe(land.getAddress()),
                nullSafe(land.getRegionSido()), nullSafe(land.getRegionSigungu()), nullSafe(land.getRegionEupmyeondong()),
                nullSafe(land.getLcCodeNm()), value(land.getArea()), value(land.getDesiredArea()),
                nullSafe(land.getRegstrSeCodeNm()), nullSafe(land.getCnrsPsnCo()),
                land.getDesiredPrice() == null ? "미제공" : String.format("%,d원", land.getDesiredPrice()),
                transactionTypeKo(land), nullSafe(land.getDescription()),
                nullSafe(land.getPrposAreaName()), nullSafe(land.getPrposAreaCode()), nullSafe(land.getPrposAreaCnflcAtNm()),
                joinZones(land), joinEtcs(land),
                scoreText(s.getSunlightScore()), scoreText(s.getSlopeScore()), scoreText(s.getAreaScore()),
                scoreText(s.getRoadScore()), scoreText(s.getRegulationScore()), scoreText(s.getZoneScore()),
                scoreText(s.getTotalScore()), nullSafe(s.getGrade()), s.isDisqualified(), nullSafe(s.getRationale()));

        String response = chatClient.prompt().user(prompt).call().content();
        Map<String, String> summaries = parseKeyValues(response);

        SolarSummaryReportResponse result = new SolarSummaryReportResponse(
                land.getId(), land.getAddress(), s.getTotalScore(), s.getGrade(), s.isDisqualified(),
                summaryText(summaries, "FAVORABLE_CONDITIONS"),
                summaryText(summaries, "CONSTRAINTS"),
                summaryText(summaries, "ADDITIONAL_CHECKS"));

        // 캐시 저장
        try {
            land.updateSolarSummaryCache(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("태양광 요약 캐시 저장 실패: {}", e.getMessage());
        }

        return result;
    }

    private String buildCriteriaPrompt(Land land, DevelopmentScoreResponse scores, List<Criterion> criteria) {
        StringBuilder sb = new StringBuilder("""
                당신은 한국 태양광 입지 적합도 분석가입니다. 각 기준의 점수와 제공된 토지 자료를 해석해 한국어 약 2문장으로 설명하세요.
                현재 평가 근거 → 그 의미 → 태양광 설치 관점의 평가 순으로 작성하세요.
                실제 측정값이 없는 경사도·일사량·도로거리는 지어내지 말고 점수와 산정 근거 범위에서 설명하며 현장 확인 필요성을 밝혀주세요.
                규제 및 용도지역은 코드 기반 사전검토이며 법률적 확정 판단처럼 표현하지 마세요.
                아래 키 형식만 출력하고 값은 한 줄로 작성하세요.
                """);
        sb.append("\n토지: ").append(nullSafe(land.getAddress())).append(" / 지목: ")
                .append(nullSafe(land.getLcCodeNm())).append(" / 면적: ").append(value(land.getArea()))
                .append("㎡ / 희망면적: ").append(value(land.getDesiredArea())).append("㎡")
                .append("\n용도지역: ").append(nullSafe(land.getPrposAreaName()))
                .append(" / 용도지구: ").append(joinZones(land)).append(" / 기타규제: ").append(joinEtcs(land))
                .append("\n산정 근거: ").append(nullSafe(scores.getRationale())).append("\n");
        for (int i = 0; i < criteria.size(); i++) {
            Criterion c = criteria.get(i);
            sb.append("CRITERION_").append(i + 1).append(": ").append(c.name).append(" ")
                    .append(scoreText(c.score)).append("/").append(c.maxScore).append("\n");
        }
        return sb.toString();
    }

    private String buildPrompt(Land land) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 토지 투자 전문 분석가입니다.\n아래 토지 정보를 바탕으로 투자 가치 분석 보고서를 작성해주세요.\n\n");
        sb.append("## 토지 기본 정보\n");
        sb.append("- 주소: ").append(land.getAddress()).append("\n");
        sb.append("- 지역: ").append(nullSafe(land.getRegionSido())).append(" ").append(nullSafe(land.getRegionSigungu())).append(" ").append(nullSafe(land.getRegionEupmyeondong())).append("\n");
        sb.append("- 면적: ").append(land.getArea() != null ? land.getArea() + "㎡" : "미제공").append("\n");
        sb.append("- 지목: ").append(nullSafe(land.getLcCodeNm())).append("\n");
        sb.append("- 대장구분: ").append(nullSafe(land.getRegstrSeCodeNm())).append("\n");
        sb.append("- 공유인수: ").append(nullSafe(land.getCnrsPsnCo())).append("명\n");
        sb.append("\n## 거래 정보\n");
        sb.append("- 거래유형: ").append(transactionTypeKo(land)).append("\n");
        sb.append("- 희망가격: ").append(land.getDesiredPrice() != null ? String.format("%,d원", land.getDesiredPrice()) : "미제공").append("\n");
        if (land.getDescription() != null && !land.getDescription().isBlank()) {
            sb.append("\n## 소유자 설명\n").append(land.getDescription()).append("\n");
        }
        if (land.getPrposAreaCnflcAtNm() != null && !land.getPrposAreaCnflcAtNm().isBlank()) {
            sb.append("\n## 용도지역 정보\n- 용도저촉여부: ").append(land.getPrposAreaCnflcAtNm()).append("\n");
        }
        if (!land.getLandZones().isEmpty()) {
            sb.append("\n## 용도지역·지구 목록\n");
            land.getLandZones().forEach(z -> sb.append("- ").append(z.toString()).append("\n"));
        }
        if (!land.getLandEtcs().isEmpty()) {
            sb.append("\n## 기타 규제 사항\n");
            land.getLandEtcs().forEach(e -> sb.append("- ").append(e.toString()).append("\n"));
        }
        sb.append("\n## 작성 지침\n1. 투자 가치 요약 (3줄 이내)\n2. 장점 (3가지)\n3. 리스크 요인 (2~3가지)\n4. 종합 의견\n");
        sb.append("\n수치와 사실에 기반하여 객관적으로 작성하세요. 마크다운 형식으로 작성해주세요.\n");
        return sb.toString();
    }

    private Land getLand(Long landId) {
        return landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
    }

    private Map<String, String> parseKeyValues(String response) {
        Map<String, String> result = new HashMap<>();
        if (response != null) for (String line : response.split("\\R")) {
            int sep = line.indexOf(':');
            if (sep > 0) result.put(line.substring(0, sep).trim(), line.substring(sep + 1).trim());
        }
        return result;
    }

    private String summaryText(Map<String, String> summaries, String key) {
        String text = summaries.get(key);
        return text == null || text.isBlank() ? "AI 평가 결과를 해석하지 못했습니다." : text;
    }

    private boolean hasSummarySections(SolarSummaryReportResponse summary) {
        return summary.getFavorableConditions() != null && !summary.getFavorableConditions().isBlank()
                && summary.getConstraints() != null && !summary.getConstraints().isBlank()
                && summary.getAdditionalChecks() != null && !summary.getAdditionalChecks().isBlank();
    }

    private String joinZones(Land land) {
        return land.getLandZones().isEmpty() ? "미제공" :
                land.getLandZones().stream().map(z -> z.getCodeName() != null ? z.getCodeName() : z.getCode())
                        .reduce((a, b) -> a + ", " + b).orElse("미제공");
    }

    private String joinEtcs(Land land) {
        return land.getLandEtcs().isEmpty() ? "미제공" :
                land.getLandEtcs().stream().map(e -> e.getCodeName() != null ? e.getCodeName() : e.getCode())
                        .reduce((a, b) -> a + ", " + b).orElse("미제공");
    }

    private String value(Double v) { return v == null ? "미제공" : v.toString(); }
    private String scoreText(Integer score) { return score == null ? "부적합/미산정" : score.toString(); }
    private record Criterion(String name, Integer score, int maxScore) {}
    private String nullSafe(String v) { return v != null ? v : "미제공"; }
    private String transactionTypeKo(Land land) {
        if (land.getTransactionType() == null) return "미제공";
        return switch (land.getTransactionType()) {
            case SALE -> "매매"; case LEASE -> "임대"; case BUSINESS_HOPE -> "사업제안";
        };
    }
}
