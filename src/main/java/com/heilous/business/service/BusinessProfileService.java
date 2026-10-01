package com.heilous.business.service;

import com.heilous.business.dto.BusinessProfileRequest;
import com.heilous.business.dto.BusinessProfileResponse;
import com.heilous.business.entity.BusinessProfile;
import com.heilous.business.repository.BusinessProfileRepository;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.common.service.ImageStorageService;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserPlan;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    // 사업자 프로필 등록/수정 (JSON만)
    @Transactional
    public BusinessProfileResponse saveProfile(String email, BusinessProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (user.getPlan() != UserPlan.BUSINESS) {
            throw new CustomException(GlobalErrorCode.PLAN_REQUIRED);
        }

        businessProfileRepository.findByUserEmail(email).ifPresentOrElse(
                existing -> {
                    if (!existing.getBusinessNumber().equals(request.getBusinessNumber())
                            && businessProfileRepository.existsByBusinessNumber(request.getBusinessNumber())) {
                        throw new CustomException(GlobalErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
                    }
                    existing.update(request.getCompanyName(), request.getBusinessNumber(),
                            request.getDepartment(), request.getJobTitle(),
                            request.getMainFields(), request.getMainFieldDescription(),
                            request.getBusinessDescription(), request.getActiveRegions(),
                            request.isNationwideActive(), request.isConsultationAvailable(),
                            request.getConsultationStartTime(), request.getConsultationEndTime());
                },
                () -> {
                    if (businessProfileRepository.existsByBusinessNumber(request.getBusinessNumber())) {
                        throw new CustomException(GlobalErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
                    }
                    businessProfileRepository.save(BusinessProfile.builder()
                            .user(user)
                            .companyName(request.getCompanyName())
                            .businessNumber(request.getBusinessNumber())
                            .department(request.getDepartment())
                            .jobTitle(request.getJobTitle())
                            .mainFields(request.getMainFields())
                            .mainFieldDescription(request.getMainFieldDescription())
                            .businessDescription(request.getBusinessDescription())
                            .activeRegions(request.getActiveRegions())
                            .nationwideActive(request.isNationwideActive())
                            .consultationAvailable(request.isConsultationAvailable())
                            .consultationStartTime(request.getConsultationStartTime())
                            .consultationEndTime(request.getConsultationEndTime())
                            .build());
                }
        );

        return BusinessProfileResponse.from(
                businessProfileRepository.findByUserEmail(email)
                        .orElseThrow(() -> new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR))
        );
    }

    // 사업자 인증 파일 업로드
    @Transactional
    public BusinessProfileResponse uploadLicense(String email, MultipartFile licenseFile) {
        BusinessProfile profile = businessProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.BUSINESS_PROFILE_NOT_FOUND));

        imageStorageService.delete(profile.getBusinessLicensePath(), "licenses");
        profile.updateLicensePath(imageStorageService.store(licenseFile, "licenses"));

        return BusinessProfileResponse.from(profile);
    }

    // 내 사업자 프로필 조회
    @Transactional(readOnly = true)
    public BusinessProfileResponse getMyProfile(String email) {
        BusinessProfile profile = businessProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        return BusinessProfileResponse.from(profile);
    }

    // 전체 사업자 프로필 조회
    @Transactional(readOnly = true)
    public List<BusinessProfileResponse> getAllProfiles() {
        return businessProfileRepository.findAll().stream()
                .map(BusinessProfileResponse::from)
                .toList();
    }

    // id로 사업자 프로필 상세 조회
    @Transactional(readOnly = true)
    public BusinessProfileResponse getProfileById(Long id) {
        BusinessProfile profile = businessProfileRepository.findById(id)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        return BusinessProfileResponse.from(profile);
    }
}
