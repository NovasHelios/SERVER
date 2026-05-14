package com.heilous.apply.controller;

import com.heilous.apply.dto.ApplyRequest;
import com.heilous.apply.dto.ApplyResponse;
import com.heilous.apply.service.ApplyService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Apply", description = "토지 신청 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/applies")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;

    @Operation(summary = "토지 신청", description = "COMPANY 권한 필요")
    @PostMapping("/{landId}")
    public APIResponse<String> applyLand(
            @PathVariable Long landId,
            @RequestBody ApplyRequest request,
            @AuthenticationPrincipal String email
    ) {

        applyService.applyLand(
                landId,
                request,
                email
        );

        return APIResponse.ok("토지 신청 완료");
    }

    @Operation(summary = "토지별 신청 목록 조회", description = "토지 소유자만 조회 가능")
    @GetMapping("/{landId}")
    public APIResponse<List<ApplyResponse>> getApplies(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {

        return APIResponse.ok(
                applyService.getApplies(
                        landId,
                        email
                )
        );
    }

    @Operation(summary = "내 신청 내역 조회")
    @GetMapping("/me")
    public APIResponse<List<ApplyResponse>> myApplies(
            @AuthenticationPrincipal String email
    ) {

        return APIResponse.ok(
                applyService.myApplies(email)
        );
    }

    @Operation(summary = "신청 승인", description = "토지 소유자만 승인 가능")
    @PatchMapping("/{applyId}/approve")
    public APIResponse<String> approveApply(
            @PathVariable Long applyId,
            @AuthenticationPrincipal String email
    ) {

        applyService.approveApply(
                applyId,
                email
        );

        return APIResponse.ok("신청 승인 완료");
    }

    @Operation(summary = "신청 거절", description = "토지 소유자만 거절 가능")
    @PatchMapping("/{applyId}/reject")
    public APIResponse<String> rejectApply(
            @PathVariable Long applyId,
            @AuthenticationPrincipal String email
    ) {

        applyService.rejectApply(
                applyId,
                email
        );

        return APIResponse.ok("신청 거절 완료");
    }
}