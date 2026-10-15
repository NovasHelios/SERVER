package com.heilous.ai.service;

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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final ChatClient chatClient;
    private final LandRepository landRepository;
    private final LandAnalysisService landAnalysisService;

    /**
     * 토지 정보를 기반으로 AI 분석 보고서를 생성합니다.
     * temperature=0 으로 설정되어 동일한 입력 → 동일한 출력이 보장됩니다.
     */
    @Transactional(readOnly = true)
    public AiReportResponse generateReport(Long landId) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        String prompt = buildPrompt(land);

        String report = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new AiReportResponse(land.getId(), land.getAddress(), report);
    }

    @Transactional(readOnly = true)
    public SolarSuitabilityReportResponse generateSolarSuitabilityReport(Long landId) {
        Land land = getLand(landId);
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
        return new SolarSuitabilityReportResponse(land.getId(), land.getAddress(), reports);
    }

    @Transactional(readOnly = true)
    public SolarSummaryReportResponse generateSolarSummaryReport(Long landId) {
        Land land = getLand(landId);
        DevelopmentScoreResponse s = landAnalysisService.analyzeDevelopment(landId);
        String prompt = """
                당신은 한국 토지의 태양광 설치 후보 적합성을 설명하는 분석가입니다.
                제공한 토지 상세정보와 적합도 결과만 근거로 한국어 6~7문장의 종합 보고서를 작성하세요.
                여러 조건을 함께 평가하고 유리한 조건, 제약, 추가 확인사항을 균형 있게 포함하세요.
                주어지지 않은 현장 사실(실제 경사도, 일사량, 도로까지 거리 등)은 추정해 사실처럼 쓰지 말고 확인이 필요하다고 명시하세요.
                부적합이면 그 사유를 분명히 설명하고 가능성을 과장하지 마세요. 제목이나 목록 없이 문단만 반환하세요.

                [토지 상세정보]
                주소: %s
                지역: %s %s %s
                지목/면적: %s / %s㎡ (희망 면적: %s㎡)
                대장 구분/공유인수: %s / %s
                희망가격/거래유형: %s / %s
                소유자 설명: %s
                용도지역: %s (%s), 저촉여부: %s
                용도지구: %s
                기타 규제: %s
                [적합도 결과]
                일사량 %s/25, 경사도 %s/20, 개발 가능 면적 %s/10, 도로 접근성 %s/10,
                토지이용규제 %s/20, 용도지역·용도지구 %s/15
                총점: %s/100, 등급: %s, 부적합: %s
                항목별 산정 근거: %s
                """.formatted(nullSafe(land.getAddress()), nullSafe(land.getRegionSido()),
                nullSafe(land.getRegionSigungu()), nullSafe(land.getRegionEupmyeondong()),
                nullSafe(land.getLcCodeNm()), value(land.getArea()), value(land.getDesiredArea()),
                nullSafe(land.getRegstrSeCodeNm()), nullSafe(land.getCnrsPsnCo()),
                land.getDesiredPrice() == null ? "미제공" : String.format("%,d원", land.getDesiredPrice()),
                transactionTypeKo(land), nullSafe(land.getDescription()), nullSafe(land.getPrposAreaName()),
                nullSafe(land.getPrposAreaCode()), nullSafe(land.getPrposAreaCnflcAtNm()),
                joinZones(land), joinEtcs(land), scoreText(s.getSunlightScore()), scoreText(s.getSlopeScore()),
                scoreText(s.getAreaScore()), scoreText(s.getRoadScore()), scoreText(s.getRegulationScore()),
                scoreText(s.getZoneScore()), scoreText(s.getTotalScore()), nullSafe(s.getGrade()),
                s.isDisqualified(), nullSafe(s.getRationale()));
        String report = chatClient.prompt().user(prompt).call().content();
        return new SolarSummaryReportResponse(land.getId(), land.getAddress(), s.getTotalScore(), s.getGrade(),
                s.isDisqualified(), report == null ? "" : report.trim());
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

    private Land getLand(Long landId) {
        return landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));
    }

    private Map<String, String> parseKeyValues(String response) {
        Map<String, String> result = new HashMap<>();
        if (response != null) for (String line : response.split("\\R")) {
            int separator = line.indexOf(':');
            if (separator > 0) result.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
        }
        return result;
    }

    private String joinZones(Land land) {
        return land.getLandZones().isEmpty() ? "미제공" : land.getLandZones().stream()
                .map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("미제공");
    }
    private String joinEtcs(Land land) {
        return land.getLandEtcs().isEmpty() ? "미제공" : land.getLandEtcs().stream()
                .map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("미제공");
    }
    private String value(Double value) { return value == null ? "미제공" : value.toString(); }
    private String scoreText(Integer score) { return score == null ? "부적합/미산정" : score.toString(); }
    private record Criterion(String name, Integer score, int maxScore) {}

    /**
     * 토지 정보를 구조화된 프롬프트로 변환합니다.
     * 동일한 토지에 대해 항상 동일한 프롬프트가 생성되어 일관된 답변을 보장합니다.
     */
    private String buildPrompt(Land land) {
        StringBuilder sb = new StringBuilder();

        sb.append("당신은 토지 투자 전문 분석가입니다.\n");
        sb.append("아래 토지 정보를 바탕으로 투자 가치 분석 보고서를 작성해주세요.\n\n");

        sb.append("## 토지 기본 정보\n");
        sb.append("- 주소: ").append(land.getAddress()).append("\n");
        sb.append("- 지역: ").append(nullSafe(land.getRegionSido()))
          .append(" ").append(nullSafe(land.getRegionSigungu()))
          .append(" ").append(nullSafe(land.getRegionEupmyeondong())).append("\n");
        sb.append("- 면적: ").append(land.getArea() != null ? land.getArea() + "㎡" : "미제공").append("\n");
        sb.append("- 지목: ").append(nullSafe(land.getLcCodeNm())).append("\n");
        sb.append("- 대장구분: ").append(nullSafe(land.getRegstrSeCodeNm())).append("\n");
        sb.append("- 공유인수: ").append(nullSafe(land.getCnrsPsnCo())).append("명\n");

        sb.append("\n## 거래 정보\n");
        sb.append("- 거래유형: ").append(transactionTypeKo(land)).append("\n");
        sb.append("- 희망가격: ").append(land.getDesiredPrice() != null
                ? String.format("%,d원", land.getDesiredPrice()) : "미제공").append("\n");

        if (land.getDescription() != null && !land.getDescription().isBlank()) {
            sb.append("\n## 소유자 설명\n");
            sb.append(land.getDescription()).append("\n");
        }

        if (land.getPrposAreaCnflcAtNm() != null && !land.getPrposAreaCnflcAtNm().isBlank()) {
            sb.append("\n## 용도지역 정보\n");
            sb.append("- 용도저촉여부: ").append(land.getPrposAreaCnflcAtNm()).append("\n");
        }

        if (!land.getLandZones().isEmpty()) {
            sb.append("\n## 용도지역·지구 목록\n");
            land.getLandZones().forEach(z ->
                sb.append("- ").append(z.toString()).append("\n")
            );
        }

        if (!land.getLandEtcs().isEmpty()) {
            sb.append("\n## 기타 규제 사항\n");
            land.getLandEtcs().forEach(e ->
                sb.append("- ").append(e.toString()).append("\n")
            );
        }

        sb.append("\n## 작성 지침\n");
        sb.append("1. 투자 가치 요약 (3줄 이내)\n");
        sb.append("2. 장점 (3가지)\n");
        sb.append("3. 리스크 요인 (2~3가지)\n");
        sb.append("4. 종합 의견\n");
        sb.append("\n수치와 사실에 기반하여 객관적으로 작성하세요. 마크다운 형식으로 작성해주세요.\n");

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null ? value : "미제공";
    }

    private String transactionTypeKo(Land land) {
        if (land.getTransactionType() == null) return "미제공";
        return switch (land.getTransactionType()) {
            case SALE     -> "매매";
            case LEASE    -> "임대";
            case BUSINESS_HOPE -> "사업제안";
        };
    }
}
