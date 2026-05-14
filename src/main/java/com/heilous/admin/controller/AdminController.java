package com.heilous.admin.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.service.LandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LandService landService;

    @Operation(summary = "토지 승인", description = "ADMIN 권한 필요")
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

    @Operation(summary = "토지 거절", description = "ADMIN 권한 필요")
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