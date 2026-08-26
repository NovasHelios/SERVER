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
@CrossOrigin
@RequestMapping("/api/applies")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;

    @Operation(summary = "토지 신청(COMPANY) — 기업 사용자가 특정 토지에 매수/임대 신청을 등록합니다.")
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

    @Operation(summary = "토지별 신청 목록 조회(USER) — 특정 토지에 접수된 기업의 매수/임대 신청 내역을 조회합니다.")
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

    @Operation(summary = "내 신청 내역 조회(COMPANY) — 로그인한 기업 사용자가 본인이 신청한 내역들을 조회합니다.")
    @GetMapping("/me")
    public APIResponse<List<ApplyResponse>> myApplies(
            @AuthenticationPrincipal String email
    ) {

        return APIResponse.ok(
                applyService.myApplies(email)
        );
    }

    @Operation(summary = "신청 승인(USER) — 토지 소유자(개인)가 접수된 신청을 승인합니다.")
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

    @Operation(summary = "신청 거절(USER) — 토지 소유자(개인)가 접수된 신청을 거절합니다.")
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