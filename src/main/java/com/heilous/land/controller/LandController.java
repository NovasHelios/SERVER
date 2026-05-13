package com.heilous.land.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.service.LandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lands")
@RequiredArgsConstructor
public class LandController {

    private final LandService landService;

    // 등록
    @PostMapping
    public APIResponse<String> registerLand(
            @RequestBody LandRegisterRequest request,
            @AuthenticationPrincipal String email
    ) {

        landService.registerLand(request, email);

        return APIResponse.ok("토지 등록 완료");
    }

    // 전체 조회
    @GetMapping
    public APIResponse<List<LandResponse>> getAllLands() {

        return APIResponse.ok(
                landService.getAllLands()
        );
    }

    // 상세 조회
    @GetMapping("/{landId}")
    public APIResponse<LandResponse> getLand(
            @PathVariable Long landId
    ) {

        return APIResponse.ok(
                landService.getLand(landId)
        );
    }

    // 상태 조회
    @GetMapping("/status/{status}")
    public APIResponse<List<LandResponse>> getLandsByStatus(
            @PathVariable String status
    ) {

        return APIResponse.ok(
                landService.getLandsByStatus(status)
        );
    }

    // 수정
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

    // 삭제
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

    // 승인
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

    // 거절
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