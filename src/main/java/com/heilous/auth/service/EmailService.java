package com.heilous.auth.service;

import com.heilous.common.dto.APIResponse;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    public void sendVerificationEmail(String email) {

        if (userRepository.existsByEmail(email)) {
            throw new CustomException(GlobalErrorCode.EMAIL_ALREADY_EXISTS);
        }

        sendCode(email, "EMAIL_AUTH:", "Heilous 회원가입 인증 코드");
    }

    public void sendPasswordResetEmail(String email) {

        if (!userRepository.existsByEmail(email)) {
            throw new CustomException(GlobalErrorCode.USER_NOT_FOUND);
        }

        sendCode(email, "PASSWORD_RESET_AUTH:", "Heilous 비밀번호 재설정 인증 코드");
    }

    private void sendCode(String email, String redisPrefix, String subject) {

        String authNum = String.valueOf(
                100000 + new Random().nextInt(900000)
        );

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, false, "utf-8");

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText("인증번호 : " + authNum, false);

            javaMailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("메일 전송 실패");
        }

        redisTemplate.opsForValue().set(
                redisPrefix + email,
                authNum,
                5,
                TimeUnit.MINUTES
        );
    }

    public APIResponse<String> checkEmailCode(
            String email,
            String code
    ) {

        String savedCode =
                redisTemplate.opsForValue()
                        .get("EMAIL_AUTH:" + email);

        if (savedCode != null && savedCode.equals(code)) {

            // 인증 성공 저장
            redisTemplate.opsForValue().set(
                    "EMAIL_VERIFIED:" + email,
                    "true",
                    30,
                    TimeUnit.MINUTES
            );

            return APIResponse.ok("인증 성공");
        }

        throw new CustomException(
                GlobalErrorCode.EMAIL_VERIFICATION_FAILED
        );
    }

    public void checkPasswordResetCode(String email, String code) {

        String savedCode = redisTemplate.opsForValue()
                .get("PASSWORD_RESET_AUTH:" + email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new CustomException(GlobalErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        // 인증 성공 → 비밀번호 변경 가능 상태로 저장 (10분)
        redisTemplate.opsForValue().set(
                "PASSWORD_RESET_VERIFIED:" + email,
                "true",
                10,
                TimeUnit.MINUTES
        );

        redisTemplate.delete("PASSWORD_RESET_AUTH:" + email);
    }
}