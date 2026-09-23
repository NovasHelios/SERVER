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

    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "회원가입 전 이메일 인증을 위해 6자리 인증 코드를 발송합니다. 코드는 Redis에 저장되며 일정 시간 후 만료됩니다."
    )
    @PostMapping("/email/send")
    public APIResponse<String> sendEmail(
            @RequestParam String email
    ) {

        emailService.sendVerificationEmail(email);

        return APIResponse.ok(
                "인증 코드가 발송되었습니다."
        );
    }

    @Operation(
            summary = "이메일 인증 코드 재전송",
            description = "기존에 발송된 인증 코드를 무효화하고 새로운 6자리 코드를 재발송합니다. 코드를 받지 못했거나 만료된 경우 사용합니다."
    )
    @PostMapping("/email/resend")
    public APIResponse<String> resendEmail(
            @RequestParam String email
    ) {
        emailService.resendVerificationEmail(email);
        return APIResponse.ok("인증 코드가 재발송되었습니다.");
    }

    @Operation(
            summary = "이메일 인증 코드 확인",
            description = "발송된 6자리 코드와 사용자가 입력한 코드가 일치하는지 검증합니다. 인증 성공 시 해당 이메일로 회원가입이 가능해집니다."
    )
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

    @Operation(
            summary = "회원가입",
            description = "이메일 인증 완료 후 사용자 계정을 생성합니다. role은 USER(개인 토지 소유자) 또는 COMPANY(기업) 중 하나를 선택하며, ADMIN은 직접 등록합니다."
    )
    @PostMapping("/signup")
    public APIResponse<String> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {

        authService.signUp(request);

        return APIResponse.ok(
                "회원가입이 완료되었습니다."
        );
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. 성공 시 이후 인증이 필요한 API 호출에 사용할 JWT Bearer 토큰을 반환합니다."
    )
    @PostMapping("/login")
    public APIResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return APIResponse.ok(authService.login(request));
    }

    // ── 비밀번호 찾기 ──────────────────────────────────────────

    @Operation(
            summary = "비밀번호 재설정 인증 코드 발송",
            description = "비밀번호 찾기 시 가입된 이메일로 6자리 재설정 인증 코드를 발송합니다. 가입되지 않은 이메일이면 오류를 반환합니다."
    )
    @PostMapping("/password/send")
    public APIResponse<String> sendPasswordResetEmail(
            @RequestParam String email
    ) {
        emailService.sendPasswordResetEmail(email);
        return APIResponse.ok("인증 코드가 발송되었습니다.");
    }

    @Operation(
            summary = "비밀번호 재설정 코드 인증",
            description = "발송된 재설정 코드를 검증합니다. 코드 일치 시 해당 이메일에 대해 비밀번호 변경 가능 상태로 전환되며, 이후 /password/reset을 호출할 수 있습니다."
    )
    @PostMapping("/password/verify")
    public APIResponse<String> verifyPasswordResetCode(
            @RequestParam String email,
            @RequestParam String code
    ) {
        emailService.checkPasswordResetCode(email, code);
        return APIResponse.ok("인증 성공. 비밀번호를 변경해주세요.");
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "코드 인증 완료 후 새 비밀번호로 변경합니다. 인증 단계를 거치지 않은 요청은 거부됩니다."
    )
    @PostMapping("/password/reset")
    public APIResponse<String> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.resetPassword(request);
        return APIResponse.ok("비밀번호가 변경되었습니다.");
    }
}