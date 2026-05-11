package com.heilous.auth.controller;

import com.heilous.auth.dto.SignUpRequest;
import com.heilous.auth.service.AuthService;
import com.heilous.auth.service.EmailService;
import com.heilous.common.dto.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    // 이메일 인증코드 발송
    @PostMapping("/email/send")
    public APIResponse<String> sendEmail(@RequestParam String email) {
        emailService.sendVerificationEmail(email);
        return APIResponse.ok("인증 코드가 발송되었습니다.");
    }

    // 이메일 인증코드 확인
    @PostMapping("/email/verify")
    public APIResponse<String> verifyEmail(
            @RequestParam String email,
            @RequestParam String code
    ) {
        return emailService.checkEmailCode(email, code);
    }

    // 회원가입
    @PostMapping("/signup")
    public APIResponse<String> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {
        authService.signUp(request);
        return APIResponse.ok("회원가입이 완료되었습니다.");
    }
}