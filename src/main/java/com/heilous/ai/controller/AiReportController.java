package com.heilous.ai.controller;

import com.heilous.ai.dto.AiReportResponse;
import com.heilous.ai.dto.SolarSuitabilityReportResponse;
import com.heilous.ai.dto.SolarSummaryReportResponse;
import com.heilous.ai.service.AiReportService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;

    @Operation(
            summary = "토지 AI 보고서 생성",
            description = "토지 정보를 기반으로 GPT-4o가 투자 가치 분석 보고서를 생성합니다. " +
                          "temperature=0 으로 설정되어 동일한 토지에 대해 항상 일관된 답변이 반환됩니다."
    )
    @GetMapping("/report")
    public APIResponse<AiReportResponse> generateReport(
            @RequestParam Long landId
    ) {
        return APIResponse.ok(aiReportService.generateReport(landId));
    }

    @Operation(summary = "AI 태양광 적합도 보고서 생성",
            description = "태양광 적합도 평가 항목별 점수와 근거를 바탕으로 기준별 설명을 생성합니다.")
    @GetMapping("/solar/suitability")
    public APIResponse<SolarSuitabilityReportResponse> generateSolarSuitabilityReport(
            @RequestParam Long landId
    ) {
        return APIResponse.ok(aiReportService.generateSolarSuitabilityReport(landId));
    }

    @Operation(summary = "AI 태양광 종합 보고서 생성",
            description = "토지 상세정보와 태양광 적합도 결과를 종합한 6~7문장 보고서를 생성합니다.")
    @GetMapping("/solar/summary")
    public APIResponse<SolarSummaryReportResponse> generateSolarSummaryReport(
            @RequestParam Long landId
    ) {
        return APIResponse.ok(aiReportService.generateSolarSummaryReport(landId));
    }
}
