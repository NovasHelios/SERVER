package com.heilous.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.heilous.business.entity.BusinessProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class BusinessProfileResponse {

    private Long id;
    private String companyName;
    private String businessNumber;
    private String department;
    private String jobTitle;
    private List<String> mainFields;
    private String mainFieldDescription;
    private String businessDescription;
    private List<String> activeRegions;
    private boolean nationwideActive;
    private boolean consultationAvailable;

    @Schema(type = "string", example = "09:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime consultationStartTime;

    @Schema(type = "string", example = "18:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime consultationEndTime;

    private String businessLicensePath;
    private boolean businessVerified;

    public static BusinessProfileResponse from(BusinessProfile p) {
        return BusinessProfileResponse.builder()
                .id(p.getId())
                .companyName(p.getCompanyName())
                .businessNumber(p.getBusinessNumber())
                .department(p.getDepartment())
                .jobTitle(p.getJobTitle())
                .mainFields(p.getMainFields())
                .mainFieldDescription(p.getMainFieldDescription())
                .businessDescription(p.getBusinessDescription())
                .activeRegions(p.getActiveRegions())
                .nationwideActive(p.isNationwideActive())
                .consultationAvailable(p.isConsultationAvailable())
                .consultationStartTime(p.getConsultationStartTime())
                .consultationEndTime(p.getConsultationEndTime())
                .businessLicensePath(p.getBusinessLicensePath())
                .businessVerified(p.isBusinessVerified())
                .build();
    }
}
