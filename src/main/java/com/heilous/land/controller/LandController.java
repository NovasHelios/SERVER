package com.heilous.land.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.service.LandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Land", description = "토지 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/lands")
@RequiredArgsConstructor
public class LandController {

    private final LandService landService;

    @Operation(summary = "토지 등록", description = "USER 권한 필요")
    @PostMapping
    public APIResponse<String> registerLand(
            @RequestBody LandRegisterRequest request,
            @AuthenticationPrincipal String email
    ) {

        landService.registerLand(request, email);

        return APIResponse.ok("토지 등록 완료");
    }

    @Operation(summary = "토지 전체 조회")
    @GetMapping
    public APIResponse<List<LandResponse>> getAllLands() {

        return APIResponse.ok(
                landService.getAllLands()
        );
    }

    @Operation(summary = "토지 상세 조회")
    @GetMapping("/{landId}")
    public APIResponse<LandResponse> getLand(
            @PathVariable Long landId
    ) {

        return APIResponse.ok(
                landService.getLand(landId)
        );
    }

    @Operation(summary = "상태별 토지 조회", description = "status: PENDING | APPROVED | REJECTED")
    @GetMapping("/status/{status}")
    public APIResponse<List<LandResponse>> getLandsByStatus(
            @PathVariable String status
    ) {

        return APIResponse.ok(
                landService.getLandsByStatus(status)
        );
    }

    @Operation(summary = "토지 수정", description = "본인 소유 토지만 수정 가능")
    @PatchMapping("/{landId}")
    public APIResponse<String> updateLand(
            @PathVariable Long landId,
            @RequestBody LandUpdateRequest request,
            @AuthenticationPrincipal String email
    ) {

        landService.updateLand(
                landId,
                request,
                email
        );

        return APIResponse.ok("토지 수정 완료");
    }

    @Operation(summary = "토지 삭제", description = "본인 또는 ADMIN만 삭제 가능")
    @DeleteMapping("/{landId}")
    public APIResponse<String> deleteLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {

        landService.deleteLand(
                landId,
                email
        );

        return APIResponse.ok("토지 삭제 완료");
    }

    @Operation(summary = "토지 승인", description = "ADMIN 권한 필요")
    @PatchMapping("/{landId}/approve")
    public APIResponse<String> approveLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {

        landService.approveLand(
                landId,
                email
        );

        return APIResponse.ok("토지 승인 완료");
    }

    @Operation(summary = "토지 거절", description = "ADMIN 권한 필요")
    @PatchMapping("/{landId}/reject")
    public APIResponse<String> rejectLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {

        landService.rejectLand(
                landId,
                email
        );

        return APIResponse.ok("토지 거절 완료");
    }
}