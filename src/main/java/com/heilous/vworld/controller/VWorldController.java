package com.heilous.vworld.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.vworld.dto.AddressLandResponse;
import com.heilous.vworld.dto.VWorldLandRequest;
import com.heilous.vworld.dto.VWorldLandResponse;
import com.heilous.vworld.service.VWorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "VWorld", description = "국가중점데이터 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@CrossOrigin
@RequestMapping("/api/vworld")
@RequiredArgsConstructor
@Validated
public class VWorldController {

    private final VWorldService vWorldService;

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

    @Operation(
            summary = "주소로 토지 임야 정보 조회",
            description = "지번 또는 도로명 주소를 입력하면 카카오 주소 API로 PNU를 생성하고 토지 임야 정보를 한 번에 반환합니다."
    )
    @GetMapping("/land")
    public APIResponse<AddressLandResponse> getLandInfoByAddress(
            @Parameter(description = "지번 또는 도로명 주소", example = "서울 종로구 세종대로 172")
            @RequestParam @NotBlank(message = "주소는 필수입니다") String address
    ) {
        return APIResponse.ok(
                vWorldService.getLandInfoByAddress(address)
        );
    }
}
