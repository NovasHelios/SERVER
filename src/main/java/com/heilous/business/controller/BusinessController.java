package com.heilous.business.controller;

import com.heilous.business.dto.BusinessProfileRequest;
import com.heilous.business.dto.BusinessProfileResponse;
import com.heilous.business.service.BusinessProfileService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Business", description = "사업자 프로필 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@CrossOrigin
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessProfileService businessProfileService;

    @Operation(summary = "사업자 프로필 등록/수정 — BUSINESS 플랜 사용자만 가능합니다.")
    @PutMapping("/profile")
    public APIResponse<BusinessProfileResponse> saveProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody BusinessProfileRequest request
    ) {
        return APIResponse.ok(businessProfileService.saveProfile(email, request));
    }

    @Operation(summary = "사업자 인증 파일 업로드 — 프로필이 먼저 등록된 상태여야 합니다.")
    @PatchMapping(value = "/profile/license", consumes = "multipart/form-data")
    public APIResponse<BusinessProfileResponse> uploadLicense(
            @AuthenticationPrincipal String email,
            @RequestPart("licenseFile") MultipartFile licenseFile
    ) {
        return APIResponse.ok(businessProfileService.uploadLicense(email, licenseFile));
    }

    @Operation(summary = "내 사업자 프로필 조회")
    @GetMapping("/profile")
    public APIResponse<BusinessProfileResponse> getMyProfile(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(businessProfileService.getMyProfile(email));
    }

    @Operation(summary = "사업자 프로필 전체 조회")
    @GetMapping("/profiles")
    public APIResponse<List<BusinessProfileResponse>> getAllProfiles() {
        return APIResponse.ok(businessProfileService.getAllProfiles());
    }

    @Operation(summary = "사업자 프로필 상세 조회 — 프로필 ID로 조회합니다.")
    @GetMapping("/profiles/{id}")
    public APIResponse<BusinessProfileResponse> getProfileById(@PathVariable Long id) {
        return APIResponse.ok(businessProfileService.getProfileById(id));
    }
}
