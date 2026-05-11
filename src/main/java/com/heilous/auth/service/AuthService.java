package com.heilous.auth.service;

import com.heilous.auth.dto.SignUpRequest;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.company.entity.CompanyProfile;
import com.heilous.company.repository.CompanyProfileRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyProfileRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void signUp(SignUpRequest request) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(
                    GlobalErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        // 이메일 인증 확인
        String verified = redisTemplate.opsForValue()
                .get("EMAIL_VERIFIED:" + request.getEmail());

        if (verified == null) {
            throw new CustomException(
                    GlobalErrorCode.EMAIL_VERIFICATION_FAILED
            );
        }

        // 사업자 회원가입 검증
        if (request.getRole() == UserRole.COMPANY) {

            if (request.getCompanyName() == null ||
                    request.getBusinessNumber() == null ||
                    request.getRepresentativeName() == null) {

                throw new CustomException(
                        GlobalErrorCode.INVALID_INPUT
                );
            }

            if (companyRepository.existsByBusinessNumber(
                    request.getBusinessNumber()
            )) {
                throw new CustomException(
                        GlobalErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS
                );
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .name(request.getName())
                .phone(request.getPhone())
                .role(request.getRole())
                .isVerified(true)
                .isActive(true)
                .build();

        userRepository.save(user);

        // 사업자 프로필 저장
        if (request.getRole() == UserRole.COMPANY) {

            CompanyProfile companyProfile =
                    CompanyProfile.builder()
                            .user(user)
                            .companyName(
                                    request.getCompanyName()
                            )
                            .businessNumber(
                                    request.getBusinessNumber()
                            )
                            .representativeName(
                                    request.getRepresentativeName()
                            )
                            .build();

            companyRepository.save(companyProfile);
        }
    }
}