package com.heilous.user.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.common.service.ImageStorageService;
import com.heilous.user.dto.ChangePasswordRequest;
import com.heilous.user.dto.UpdateProfileRequest;
import com.heilous.user.dto.UserMeResponse;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(
                                GlobalErrorCode.USER_NOT_FOUND
                        )
                );

        return new UserMeResponse(
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getProfileImagePath()
        );
    }

    // 프로필 수정
    @Transactional
    public void updateProfile(
            String email,
            UpdateProfileRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(
                                GlobalErrorCode.USER_NOT_FOUND
                        )
                );

        user.updateInfo(
                request.getName(),
                request.getPhone()
        );
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(
                                GlobalErrorCode.USER_NOT_FOUND
                        )
                );

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {

            throw new CustomException(
                    GlobalErrorCode.INVALID_CREDENTIALS
            );
        }

        user.changePassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );
    }

    // 계정 삭제
    @Transactional
    public void deleteUser(
            Long id,
            String loginEmail
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new CustomException(
                                GlobalErrorCode.USER_NOT_FOUND
                        )
                );

        User currentUser =
                userRepository.findByEmail(loginEmail)
                        .orElseThrow(() ->
                                new CustomException(
                                        GlobalErrorCode.USER_NOT_FOUND
                                )
                        );

        if (!user.getEmail().equals(loginEmail)
                && currentUser.getRole() != UserRole.ADMIN) {

            throw new CustomException(
                    GlobalErrorCode.ACCESS_DENIED
            );
        }

        user.deactivate();
    }

    // 프로필 이미지 업로드
    @Transactional
    public void uploadProfileImage(String email, MultipartFile image) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 기존 이미지 삭제
        imageStorageService.delete(user.getProfileImagePath(), "profiles");

        String filename = imageStorageService.store(image, "profiles");
        user.updateProfileImage(filename);
    }
}