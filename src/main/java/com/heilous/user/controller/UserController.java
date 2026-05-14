package com.heilous.user.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.user.dto.ChangePasswordRequest;
import com.heilous.user.dto.UpdateProfileRequest;
import com.heilous.user.dto.UserMeResponse;
import com.heilous.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public APIResponse<UserMeResponse> getMyInfo(
            @AuthenticationPrincipal String email
    ) {

        return APIResponse.ok(
                userService.getMyInfo(email)
        );
    }

    @Operation(summary = "프로필 수정")
    @PatchMapping("/me")
    public APIResponse<String> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody
            UpdateProfileRequest request
    ) {

        userService.updateProfile(
                email,
                request
        );

        return APIResponse.ok(
                "프로필 수정 완료"
        );
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/password")
    public APIResponse<String> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody
            ChangePasswordRequest request
    ) {

        userService.changePassword(
                email,
                request
        );

        return APIResponse.ok(
                "비밀번호 변경 완료"
        );
    }

    @Operation(summary = "계정 삭제(비활성화)")
    @DeleteMapping("/{id}")
    public APIResponse<String> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal String loginEmail
    ) {

        userService.deleteUser(
                id,
                loginEmail
        );

        return APIResponse.ok(
                "계정이 성공적으로 삭제(비활성화)되었습니다."
        );
    }
}