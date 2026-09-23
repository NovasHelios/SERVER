package com.heilous.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "토지 태양광 개발가능성 분석 결과")
public class DevelopmentScoreResponse {

    @Schema(description = "토지 ID", example = "1")
    private Long landId;

    @Schema(description = "토지 주소")
    private String address;

    // ── 항목별 점수 ───────────────────────────────────────────
    @Schema(description = "일사량 점수 (0~25)", example = "22")
    private int sunlightScore;

    @Schema(description = "경사도 점수 (0~20 또는 부적합)", example = "17")
    private Integer slopeScore;

    @Schema(description = "개발 가능 면적 점수 (0~10)", example = "8")
    private int areaScore;

    @Schema(description = "도로 접근성 점수 (0~10 또는 부적합)", example = "10")
    private Integer roadScore;

    @Schema(description = "토지이용규제 점수 (0~20 또는 부적합)", example = "14")
    private Integer regulationScore;

    @Schema(description = "용도지역·용도지구 점수 (0~15 또는 부적합)", example = "12")
    private Integer zoneScore;

    // ── 종합 ──────────────────────────────────────────────────
    @Schema(description = "총점 (0~100). 부적합 조건 존재 시 null")
    private Integer totalScore;

    @Schema(description = "최종 적합도 등급 (매우 좋음/좋음/보통/낮음/부적합)")
    private String grade;

    @Schema(description = "부적합 여부")
    private boolean disqualified;

    // ── 근거 데이터 ────────────────────────────────────────────
    @Schema(description = "지목명")
    private String landCategory;

    @Schema(description = "면적(㎡) — API 조회값")
    private Double area;

    @Schema(description = "희망 면적(㎡) — 사용자 입력값")
    private Double desiredArea;

    @Schema(description = "용도지역 코드")
    private String prposAreaCode;

    @Schema(description = "용도지역 저촉여부")
    private String zoneConflict;

    @Schema(description = "용도지구 코드 목록 (쉼표 구분)")
    private String zoneCodes;

    @Schema(description = "기타 규제 코드 목록 (쉼표 구분)")
    private String etcCodes;

    @Schema(description = "각 항목별 점수 산출 이유 (AI 생성)")
    private String rationale;
}
