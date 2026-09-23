package com.heilous.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 개발가능성 분석 응답 DTO
 * 일조량·면적(경사) 적합도·접근성 3개 항목을 0~100점으로 환산 후
 * 가중 평균하여 개발가능성 백분위 점수를 산출합니다.
 */
@Getter
@Builder
@Schema(description = "토지 개발가능성 분석 결과")
public class DevelopmentScoreResponse {

    @Schema(description = "토지 ID", example = "1")
    private Long landId;

    @Schema(description = "토지 주소", example = "경기도 용인시 처인구 포곡읍 전대리 123")
    private String address;

    // ── 항목별 점수 (0 ~ 100) ─────────────────────────────────
    @Schema(description = "일조량 적합도 점수 (0~100). 지목·용도지역 기반으로 산출", example = "78")
    private int sunlightScore;

    @Schema(description = "면적/경사 적합도 점수 (0~100). 면적 규모·지목 기반으로 산출", example = "65")
    private int slopeScore;

    @Schema(description = "접근성 점수 (0~100). 행정구역·지목·용도지역 기반으로 산출", example = "55")
    private int accessibilityScore;

    // ── 종합 ──────────────────────────────────────────────────
    @Schema(description = "개발가능성 종합 백분위 점수 (0~100). 일조량 40% + 경사 30% + 접근성 30% 가중 평균", example = "67")
    private int developmentScore;

    @Schema(description = "개발가능성 등급 (매우높음/높음/보통/낮음/매우낮음)", example = "높음")
    private String grade;

    // ── 근거 데이터 ────────────────────────────────────────────
    @Schema(description = "지목명", example = "전")
    private String landCategory;

    @Schema(description = "면적(㎡)", example = "1200.5")
    private Double area;

    @Schema(description = "용도지역 저촉여부명", example = "포함")
    private String zoneConflict;

    @Schema(description = "주요 용도지구 코드 목록 (쉼표 구분)", example = "UQG100,UQH200")
    private String zoneCodes;

    @Schema(description = "기타 규제 코드 목록 (쉼표 구분)", example = "ZA0006")
    private String etcCodes;

    @Schema(description = "점수 산출 근거 설명")
    private String rationale;
}
