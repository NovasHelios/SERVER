package com.heilous.auth.controller;

import com.heilous.auth.dto.LoginRequest;
import com.heilous.auth.dto.LoginResponse;
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

    @Operation(summary = "이메일 인증 코드 확인")
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

    @Operation(summary = "회원가입")
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
}