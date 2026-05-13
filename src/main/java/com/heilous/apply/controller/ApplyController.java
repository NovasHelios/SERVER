package com.heilous.apply.controller;

import com.heilous.apply.dto.ApplyRequest;
import com.heilous.apply.dto.ApplyResponse;
import com.heilous.apply.service.ApplyService;
import com.heilous.common.dto.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applies")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;

    // 신청
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

    // 토지 신청 목록 조회
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

    // 내 신청 내역
    @GetMapping("/me")
    public APIResponse<List<ApplyResponse>> myApplies(
            @AuthenticationPrincipal String email
    ) {

        return APIResponse.ok(
                applyService.myApplies(email)
        );
    }

    // 승인
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

    // 거절
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