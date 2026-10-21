package com.heilous.land.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.dto.LandDetailResponse;
import com.heilous.land.dto.LandFilterRequest;
import com.heilous.land.dto.LandPageResponse;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.dto.RegionStatsResponse;
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

@Tag(name = "Land", description = "토지 API")@RestController
@CrossOrigin
@RequestMapping("/api/lands")
@RequiredArgsConstructor
public class LandController {

    private final LandService landService;

    @Operation(
            summary = "토지 등록",
            description = "USER 권한을 가진 사용자가 토지를 등록합니다. 주소를 입력하면 카카오 API와 VWorld API를 통해 면적, 지목, 법정동 등 상세 정보가 자동으로 조회되고, 개발가능성 분석 총점 및 통과 여부가 저장됩니다. 시연 중에는 등록 직후 APPROVED 상태로 처리됩니다. multipart/form-data로 전송하며 이미지는 최소 3장, 최대 5장 필수 첨부해야 합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(consumes = "multipart/form-data")
    public APIResponse<String> registerLand(
            @RequestParam("address") String address,
            @RequestParam(value = "desiredPrice", required = false) Long desiredPrice,
            @RequestParam(value = "desiredArea", required = false) Double desiredArea,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("transactionType") Land.TransactionType transactionType,
            @RequestPart(value = "document", required = false) MultipartFile document,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal String email
    ) {
        LandRegisterRequest request = new LandRegisterRequest(address, desiredPrice, desiredArea, description, transactionType);
        landService.registerLand(request, email, document, images);
        return APIResponse.ok("토지 등록 완료");
    }

    @Operation(
            summary = "시군구별 거래유형 토지 수 통계",
            description = "시/도 > 시/군/구 단위로 매매(SALE), 임대(LEASE), 사업희망(BUSINESS_HOPE) 토지 수를 반환합니다."
    )
    @GetMapping("/regions")
    public APIResponse<List<RegionStatsResponse>> getRegionStats() {
        return APIResponse.ok(landService.getRegionStats());
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

    @Operation(summary = "내 토지 목록 조회",
            description = "로그인한 USER가 본인이 등록한 토지를 20개씩 조회합니다. "
                    + "sort: n(최신순), o(오래된순), pd(희망가격 높은순), pa(희망가격 낮은순)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public APIResponse<LandPageResponse> getMyLands(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "n") String sort
    ) {
        return APIResponse.ok(landService.getMyLands(email, page, sort));
    }

    @Operation(
            summary = "토지 상세 조회",
            description = "토지 ID로 특정 토지의 상세 정보를 조회합니다. 면적, 지목, 법정동, 좌표, 용도지역, 용도지구, 기타 규제, 이미지 등 모든 필드를 반환합니다. 로그인 없이 조회 가능합니다."
    )
    @GetMapping("/{landId}")
    public APIResponse<LandDetailResponse> getLand(
            @PathVariable Long landId
    ) {

        return APIResponse.ok(
                landService.getLand(landId)
        );
    }

    @Operation(
            summary = "토지 필터 조회",
            description = "다양한 조건으로 토지를 필터링하여 조회합니다. 모든 파라미터는 선택사항이며 복합 적용 가능합니다.\n\n"
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

    @Operation(summary = "토지 분석 결제 (임시)",
            description = "실제 결제 연동 전 임시 API입니다. 토지 소유자가 호출하면 paid 값을 true로 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{landId}/payment")
    public APIResponse<String> payForLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        landService.payForLand(landId, email);
        return APIResponse.ok("결제 완료");
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
            description = "관리자가 토지를 승인합니다. 이 엔드포인트는 토지 상태(LandStatus)를 APPROVED로 변경합니다."
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
            summary = "토지 이미지 재등록",
            description = "기존 이미지를 전부 삭제하고 새 이미지 3~5장으로 전면 교체합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "/{landId}/image", consumes = "multipart/form-data")
    public APIResponse<String> replaceLandImageLegacy(
            @PathVariable Long landId,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal String email
    ) {
        landService.replaceLandImages(landId, images, email);
        return APIResponse.ok("토지 이미지 재등록 완료");
    }
}
