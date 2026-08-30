package com.heilous.land.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.dto.LandFilterRequest;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.entity.Land;
import com.heilous.land.service.LandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Land", description = "토지 API")
@RestController
@CrossOrigin
@RequestMapping("/api/lands")
@RequiredArgsConstructor
public class LandController {

    private final LandService landService;

    @Operation(
            summary = "토지 등록",
            description = "USER 권한을 가진 사용자가 토지를 등록 신청합니다. 주소를 입력하면 카카오 API와 VWorld API를 통해 면적, 지목, 법정동 등 상세 정보가 자동으로 조회됩니다. 등록된 토지는 PENDING 상태로 관리자 승인을 기다립니다. multipart/form-data로 전송하며 증명서 파일(document)을 함께 첨부할 수 있습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(consumes = "multipart/form-data")
    public APIResponse<String> registerLand(
            @RequestParam("address") String address,
            @RequestParam(value = "desiredPrice", required = false) Long desiredPrice,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("transactionType") Land.TransactionType transactionType,
            @RequestPart(value = "document", required = false) MultipartFile document,
            @AuthenticationPrincipal String email
    ) {
        LandRegisterRequest request = new LandRegisterRequest(address, desiredPrice, description, transactionType);
        landService.registerLand(request, email, document);
        return APIResponse.ok("토지 등록 완료");
    }

    @Operation(
            summary = "토지 전체 조회",
            description = "등록된 모든 토지 목록을 최신순으로 반환합니다. 로그인 없이 조회 가능합니다."
    )
    @GetMapping
    public APIResponse<List<LandResponse>> getAllLands() {

        return APIResponse.ok(
                landService.getAllLands()
        );
    }

    @Operation(
            summary = "토지 상세 조회",
            description = "토지 ID로 특정 토지의 상세 정보를 조회합니다. 면적, 지목, 법정동, 좌표, 희망가격 등 모든 필드를 반환합니다. 로그인 없이 조회 가능합니다."
    )
    @GetMapping("/{landId}")
    public APIResponse<LandResponse> getLand(
            @PathVariable Long landId
    ) {

        return APIResponse.ok(
                landService.getLand(landId)
        );
    }

    @Operation(
            summary = "토지 필터 조회",
            description = "다양한 조건으로 토지를 필터링하여 조회합니다. 모든 파라미터는 선택사항이며 복합 적용 가능합니다.\n\n"
                    + "- status: PENDING | APPROVED | REJECTED (미입력 시 전체)\n"
                    + "- transactionType: SALE | LEASE\n"
                    + "- saleMinPrice / saleMaxPrice: 매매 희망가격 범위 (원 단위)\n"
                    + "- leaseMinPrice / leaseMaxPrice: 임대 희망가격 범위 (원 단위)\n"
                    + "- minArea / maxArea: 면적 범위 (㎡ 단위)\n"
                    + "- sido: 시/도 (예: 경기도)\n"
                    + "- sigungu: 시/군/구 (sido 입력 시에만 적용, 예: 수원시)\n"
                    + "- eupmyeondong: 읍/면/동 (sigungu 입력 시에만 적용, 예: 영통동)"
    )
    @PostMapping("/filter")
    public APIResponse<List<LandResponse>> getLandsByFilter(
            @RequestBody LandFilterRequest filter
    ) {
        return APIResponse.ok(landService.getLandsByFilter(filter));
    }

    @Operation(
            summary = "토지 정보 수정",
            description = "토지 소유자(USER)만 본인 소유 토지의 주소, 희망가격, 설명, 거래유형을 수정할 수 있습니다. 주소 변경 시 VWorld API를 통해 면적, 지목, 법정동 정보가 자동으로 재조회됩니다."
    )
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "토지 삭제",
            description = "토지를 삭제합니다. 토지 소유자(USER) 본인 또는 관리자(ADMIN)만 삭제할 수 있습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "토지 등록 승인 (토지 소유자)",
            description = "토지 소유자(USER)가 해당 토지에 접수된 기업의 매수/임대 신청을 직접 승인합니다. LandController의 승인과는 별개로, 이 엔드포인트는 토지 상태(LandStatus)를 APPROVED로 변경합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "토지 등록 거절 (토지 소유자)",
            description = "토지 소유자(USER)가 해당 토지에 접수된 기업의 매수/임대 신청을 거절합니다. 토지 상태(LandStatus)가 REJECTED로 변경됩니다."
    )
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "토지 이미지 업로드",
            description = "토지 소유자(USER)가 본인 소유 토지의 대표 이미지를 업로드합니다. 기존 이미지가 있으면 자동으로 교체됩니다. 허용 형식: jpg, png, webp, gif."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "/{landId}/image", consumes = "multipart/form-data")
    public APIResponse<String> uploadLandImage(
            @PathVariable Long landId,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal String email
    ) {

        landService.uploadLandImage(landId, image, email);

        return APIResponse.ok("토지 이미지 업로드 완료");
    }
}