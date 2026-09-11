package com.heilous.ai.service;

import com.heilous.ai.dto.AiReportResponse;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final ChatClient chatClient;
    private final LandRepository landRepository;

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
