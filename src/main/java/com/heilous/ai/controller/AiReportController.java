package com.heilous.ai.controller;

import com.heilous.ai.dto.AiReportRequest;
import com.heilous.ai.dto.AiReportResponse;
import com.heilous.ai.service.AiReportService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
    @PostMapping("/report")
    public APIResponse<AiReportResponse> generateReport(
            @Valid @RequestBody AiReportRequest request
    ) {
        return APIResponse.ok(aiReportService.generateReport(request.getLandId()));
    }
}
