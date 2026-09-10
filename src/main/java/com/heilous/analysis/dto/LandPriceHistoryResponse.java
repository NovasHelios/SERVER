package com.heilous.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 연도별 공시지가 추이 응답 DTO
 * VWorld getPossessionAttr API의 pblntfPclnd(공시지가 원/㎡) 필드를
 * 연도별로 수집하여 반환합니다.
 */
@Getter
@Builder
@Schema(description = "연도별 공시지가 추이")
public class LandPriceHistoryResponse {

    @Schema(description = "토지 ID", example = "1")
    private Long landId;

    @Schema(description = "토지 주소", example = "경기도 용인시 처인구 포곡읍 전대리 123")
    private String address;

    @Schema(description = "면적(㎡)", example = "1200.5")
    private Double area;

    @Schema(description = "지목명", example = "전")
    private String landCategory;

    @Schema(description = "연도별 공시지가 목록 (오름차순)")
    private List<YearlyPrice> priceHistory;

    @Schema(description = "최근 5년 평균 상승률(%)", example = "4.3")
    private Double avgAnnualGrowthRate;

    @Schema(description = "조회 시작 연도", example = "2019")
    private int fromYear;

    @Schema(description = "조회 종료 연도", example = "2024")
    private int toYear;

    @Getter
    @Builder
    @Schema(description = "연도별 공시지가 항목")
    public static class YearlyPrice {

        @Schema(description = "기준 연도", example = "2023")
        private int year;

        @Schema(description = "공시지가 (원/㎡)", example = "1544000")
        private Long pricePerSqm;

        @Schema(description = "총 공시지가 (공시지가 × 면적, 원)", example = "1852920000")
        private Long totalPrice;

        @Schema(description = "전년 대비 변동률(%)", example = "3.5")
        private Double changeRate;
    }
}
