package com.heilous.admin.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.service.LandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LandService landService;

    // 토지 승인
    @PatchMapping("/lands/{landId}/approve")
    public APIResponse<String> approveLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {

        landService.approveLand(
                landId,
                email
        );

        return APIResponse.ok(
                "토지 승인 완료"
        );
    }

    // 토지 거절
    @PatchMapping("/lands/{landId}/reject")
    public APIResponse<String> rejectLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {

        landService.rejectLand(
                landId,
                email
        );

        return APIResponse.ok(
                "토지 거절 완료"
        );
    }
}