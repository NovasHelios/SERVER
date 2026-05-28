package com.heilous.auth.controller;

import com.heilous.auth.dto.LoginRequest;
import com.heilous.auth.dto.LoginResponse;
import com.heilous.auth.dto.PasswordResetRequest;
import com.heilous.auth.dto.SignUpRequest;
import com.heilous.auth.service.AuthService;
import com.heilous.auth.service.EmailService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@CrossOrigin
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Operation(summary = "이메일 인증 코드 발송")
    @PostMapping("/email/send")
    public APIResponse<String> sendEmail(
            @RequestParam String email
    ) {

        emailService.sendVerificationEmail(email);

        return APIResponse.ok(
                "인증 코드가 발송되었습니다."
        );
    }

    @Operation(summary = "이메일 인증 코드 확인!!!!!!")
    @PostMapping("/email/verify")
    public APIResponse<String> verifyEmail(
            @RequestParam String email,
            @RequestParam String code
    ) {

        return emailService.checkEmailCode(
                email,
                code
        );
    }

    @Operation(summary = "회원가입 role=USER / COMPANY 중 택 1 & ADMIN은 우리가 직접추가")
    @PostMapping("/signup")
    public APIResponse<String> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {

        authService.signUp(request);

        return APIResponse.ok(
                "회원가입이 완료되었습니다."
        );
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인 후 JWT 토큰 반환")
    @PostMapping("/login")
    public APIResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return APIResponse.ok(authService.login(request));
    }

    // ── 비밀번호 찾기 ──────────────────────────────────────────

    @Operation(summary = "비밀번호 재설정 인증 코드 발송", description = "가입된 이메일로 6자리 코드 발송")
    @PostMapping("/password/send")
    public APIResponse<String> sendPasswordResetEmail(
            @RequestParam String email
    ) {
        emailService.sendPasswordResetEmail(email);
        return APIResponse.ok("인증 코드가 발송되었습니다.");
    }

    @Operation(summary = "비밀번호 재설정 코드 인증", description = "코드 일치 시 비밀번호 변경 가능 상태로 전환")
    @PostMapping("/password/verify")
    public APIResponse<String> verifyPasswordResetCode(
            @RequestParam String email,
            @RequestParam String code
    ) {
        emailService.checkPasswordResetCode(email, code);
        return APIResponse.ok("인증 성공. 비밀번호를 변경해주세요.");
    }

    @Operation(summary = "비밀번호 재설정", description = "코드 인증 완료 후 새 비밀번호로 변경")
    @PostMapping("/password/reset")
    public APIResponse<String> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.resetPassword(request);
        return APIResponse.ok("비밀번호가 변경되었습니다.");
    }
}