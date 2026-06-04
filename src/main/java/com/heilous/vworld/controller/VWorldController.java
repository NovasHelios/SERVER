package com.heilous.vworld.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.vworld.dto.VWorldLandRequest;
import com.heilous.vworld.dto.VWorldLandResponse;
import com.heilous.vworld.service.VWorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "VWorld", description = "국가중점데이터 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@CrossOrigin
@RequestMapping("/api/vworld")
@RequiredArgsConstructor
public class VWorldController {

    private final VWorldService vWorldService;

    @Operation(
            summary = "토지 임야 정보 조회",
            description = "국가중점데이터 API를 통해 PNU(고유번호)로 토지 및 임야 정보를 조회합니다."
    )
    @PostMapping("/land")
    public APIResponse<VWorldLandResponse> getLandInfo(
            @Valid @RequestBody VWorldLandRequest request
    ) {
        return APIResponse.ok(
                vWorldService.getLandInfo(request)
        );
    }

    @Operation(
            summary = "토지 임야 정보 조회 (GET)",
            description = "GET 방식으로 PNU 기반 토지 정보를 조회합니다."
    )
    @GetMapping("/land/{pnu}")
    public APIResponse<VWorldLandResponse> getLandInfoByPnu(
            @PathVariable String pnu
    ) {
        VWorldLandRequest request = new VWorldLandRequest();
        request.setPnu(pnu);

        return APIResponse.ok(
                vWorldService.getLandInfo(request)
        );
    }
}
