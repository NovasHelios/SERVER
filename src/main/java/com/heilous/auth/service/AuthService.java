package com.heilous.auth.service;

import com.heilous.auth.dto.LoginRequest;
import com.heilous.auth.dto.LoginResponse;
import com.heilous.auth.dto.SignUpRequest;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.company.entity.CompanyProfile;
import com.heilous.company.repository.CompanyProfileRepository;
import com.heilous.global.auth.JwtProvider;
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
    private final JwtProvider jwtProvider;

    @Transactional
    public void signUp(SignUpRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(
                    GlobalErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        String verified = redisTemplate.opsForValue()
                .get("EMAIL_VERIFIED:" + request.getEmail());

        if (verified == null) {
            throw new CustomException(
                    GlobalErrorCode.EMAIL_VERIFICATION_FAILED
            );
        }

        if (request.getRole() == UserRole.COMPANY) {

            if (request.getCompanyName() == null ||
                    request.getBusinessNumber() == null) {

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
                            .build();

            companyRepository.save(companyProfile);
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new CustomException(
                                GlobalErrorCode.INVALID_CREDENTIALS
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new CustomException(
                    GlobalErrorCode.INVALID_CREDENTIALS
            );
        }

        if (!user.isActive()) {
            throw new CustomException(
                    GlobalErrorCode.ACCESS_DENIED
            );
        }

        return new LoginResponse(
                jwtProvider.createToken(user.getEmail(), user.getRole().name()),
                user.getEmail(),
                user.getRole().name()
        );
    }
}