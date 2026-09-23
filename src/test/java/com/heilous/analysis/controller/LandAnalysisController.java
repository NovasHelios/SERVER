package com.heilous.analysis.controller;

import com.heilous.analysis.dto.DevelopmentScoreResponse;
import com.heilous.analysis.dto.LandPriceHistoryResponse;
import com.heilous.analysis.service.LandAnalysisService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "토지 분석",
    description = """
        토지 개발가능성 분석 및 연도별 공시지가 추이 조회 API.
        
        **개발가능성 점수 산출 기준 (백분위 0~100)**
        | 항목 | 가중치 | 산출 근거 |
        |------|--------|-----------|
        | 일조량 적합도 | 40% | 지목코드, 용도지역 저촉여부, 보전임지·군사시설 등 규제 |
        | 면적/경사 적합도 | 30% | 지목코드, 면적 규모(㎡), 급경사지·보전임지 규제 |
        | 접근성 | 30% | 시도(도로망 밀도), 지목코드, 용도지역 저촉, 군사·도시개발 규제 |
        
        **등급 기준**
        | 점수 | 등급 |
        |------|------|
        | 80~100 | 매우높음 |
        | 65~79  | 높음 |
        | 45~64  | 보통 |
        | 30~44  | 낮음 |
        | 0~29   | 매우낮음 |
        
        **공시지가 추이**: VWorld getPossessionAttr API의 stdrYm(기준연월) 기반으로 연도별 최신 공시지가를 수집합니다.
        """
)
@RestController
@RequestMapping("/api/lands/{landId}/analysis")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Validated
public class LandAnalysisController {

    private final LandAnalysisService landAnalysisService;

    // ─────────────────────────────────────────────────────────
    // 1. 개발가능성 분석
    // ─────────────────────────────────────────────────────────

    @Operation(
        summary = "토지 개발가능성 분석",
        description = """
            토지 ID로 개발가능성을 백분위(0~100) 점수로 분석합니다.
            
            **일조량 점수 (40%)**: 지목이 대지·전·답인 경우 높고, 임야·하천은 낮습니다.  
            보전임지(FA), 군사시설(GF), 공원(UQP) 규제가 있으면 감점됩니다.
            
            **면적/경사 점수 (30%)**: 대지·농지는 평탄 지형으로 높게 평가됩니다.  
            330㎡ 미만 소규모는 개발 한계로 감점, 3000㎡ 이상 대규모는 가산됩니다.
            
            **접근성 점수 (30%)**: 서울·수도권은 도로망 밀도로 높게 평가됩니다.  
            군사시설(GF) 규제 시 크게 감점, 도시개발구역(DA)은 가산됩니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "분석 성공",
            content = @Content(schema = @Schema(implementation = DevelopmentScoreResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "토지를 찾을 수 없음")
    })
    @GetMapping("/development")
    public APIResponse<DevelopmentScoreResponse> getDevelopmentScore(
            @Parameter(description = "토지 ID", example = "1", required = true)
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(landAnalysisService.analyzeDevelopment(landId));
    }

    // ─────────────────────────────────────────────────────────
    // 2. 연도별 공시지가 추이
    // ─────────────────────────────────────────────────────────

    @Operation(
        summary = "연도별 공시지가 추이 조회",
        description = """
            토지 ID로 최근 N년간의 연도별 공시지가 추이를 조회합니다.
            
            **데이터 출처**: VWorld 토지소유정보 속성조회 API (getPossessionAttr)  
            **응답 필드**:
            - `pricePerSqm`: 공시지가 (원/㎡)
            - `totalPrice`: 총 공시지가 (공시지가 × 면적)
            - `changeRate`: 전년 대비 변동률 (%)
            - `avgAnnualGrowthRate`: 조회 기간 평균 연간 상승률 (%)
            
            PNU(필지고유번호)가 없는 토지는 조회할 수 없습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = LandPriceHistoryResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "토지를 찾을 수 없음"),
        @ApiResponse(responseCode = "502", description = "VWorld API 호출 실패")
    })
    @GetMapping("/price-history")
    public APIResponse<LandPriceHistoryResponse> getPriceHistory(
            @Parameter(description = "토지 ID", example = "1", required = true)
            @PathVariable Long landId,

            @Parameter(
                description = "조회 연수 (1~20, 기본값 6)",
                example = "6"
            )
            @RequestParam(defaultValue = "6")
            @Min(value = 1, message = "조회 연수는 1 이상이어야 합니다.")
            @Max(value = 20, message = "조회 연수는 20 이하여야 합니다.")
            int years,

            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(landAnalysisService.getLandPriceHistory(landId, years));
    }
}
