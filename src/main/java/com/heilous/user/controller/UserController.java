package com.heilous.user.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @DeleteMapping("/{id}")
    public APIResponse<String> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal String loginEmail) { // JWT 필터에서 넣어준 이메일 정보

        userService.deleteUser(id, loginEmail);
        return APIResponse.ok("계정이 성공적으로 삭제(비활성화)되었습니다.");
    }
}