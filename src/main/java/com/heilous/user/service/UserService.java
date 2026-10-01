package com.heilous.user.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.common.service.ImageStorageService;
import com.heilous.user.dto.ChangePlanRequest;
import com.heilous.user.dto.ChangePasswordRequest;
import com.heilous.user.dto.UpdateProfileRequest;
import com.heilous.user.dto.UserMeResponse;
import com.heilous.user.dto.UserPlanResponse;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        return new UserMeResponse(user.getEmail(), user.getName(), user.getPhone(),
                user.getRole(), user.getPlan(), user.getProfileImagePath());
    }

    @Transactional
    public void updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        user.updateInfo(request.getName(), request.getPhone());
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(GlobalErrorCode.INVALID_CREDENTIALS);
        }
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void deleteUser(Long id, String loginEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        User currentUser = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        if (!user.getEmail().equals(loginEmail) && currentUser.getRole() != UserRole.ADMIN) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }
        user.deactivate();
    }

    @Transactional
    public void uploadProfileImage(String email, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        imageStorageService.delete(user.getProfileImagePath(), "profiles");
        user.updateProfileImage(imageStorageService.store(image, "profiles"));
    }

    @Transactional
    public void changePlan(String email, ChangePlanRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        user.changePlan(request.getPlan());
    }

    @Transactional(readOnly = true)
    public List<UserPlanResponse> getAllUserPlans() {
        return userRepository.findAll().stream().map(UserPlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserPlanResponse getMyPlan(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        return UserPlanResponse.from(user);
    }
}
