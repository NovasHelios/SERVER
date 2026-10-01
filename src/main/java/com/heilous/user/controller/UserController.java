package com.heilous.user.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.user.dto.ChangePlanRequest;
import com.heilous.user.dto.ChangePasswordRequest;
import com.heilous.user.dto.UpdateProfileRequest;
import com.heilous.user.dto.UserMeResponse;
import com.heilous.user.dto.UserPlanResponse;
import com.heilous.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "User", description = "사용자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@CrossOrigin
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 정보 조회")
    @GetMapping("/me")
    public APIResponse<UserMeResponse> getMyInfo(@AuthenticationPrincipal String email) {
        return APIResponse.ok(userService.getMyInfo(email));
    }

    @Operation(summary = "내 프로필 정보 수정")
    @PatchMapping("/me")
    public APIResponse<String> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        userService.updateProfile(email, request);
        return APIResponse.ok("프로필 수정 완료");
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/password")
    public APIResponse<String> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(email, request);
        return APIResponse.ok("비밀번호 변경 완료");
    }

    @Operation(summary = "회원 탈퇴(계정 비활성화)")
    @DeleteMapping("/{id}")
    public APIResponse<String> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal String loginEmail
    ) {
        userService.deleteUser(id, loginEmail);
        return APIResponse.ok("계정이 성공적으로 삭제(비활성화)되었습니다.");
    }

    @Operation(summary = "프로필 이미지 등록/수정")
    @PatchMapping(value = "/me/image", consumes = "multipart/form-data")
    public APIResponse<String> uploadProfileImage(
            @AuthenticationPrincipal String email,
            @RequestPart("image") MultipartFile image
    ) {
        userService.uploadProfileImage(email, image);
        return APIResponse.ok("프로필 이미지 업로드 완료");
    }

    @Operation(summary = "플랜 변경 — FREE / PRO / BUSINESS 중 선택")
    @PatchMapping("/me/plan")
    public APIResponse<String> changePlan(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePlanRequest request
    ) {
        userService.changePlan(email, request);
        return APIResponse.ok("플랜이 변경되었습니다.");
    }

    @Operation(summary = "전체 유저 플랜 목록 조회 — 개발용임")
    @GetMapping("/plans")
    public APIResponse<List<UserPlanResponse>> getAllUserPlans() {
        return APIResponse.ok(userService.getAllUserPlans());
    }

    @Operation(summary = "내 플랜 조회")
    @GetMapping("/me/plan")
    public APIResponse<UserPlanResponse> getMyPlan(@AuthenticationPrincipal String email) {
        return APIResponse.ok(userService.getMyPlan(email));
    }
}