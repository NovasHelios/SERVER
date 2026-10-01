package com.heilous.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class BusinessProfileRequest {

    @NotBlank
    private String companyName;

    @NotBlank
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
}
