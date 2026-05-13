package com.heilous.apply.dto;

import com.heilous.apply.entity.ApplyStatus;
import com.heilous.apply.entity.LandApply;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplyResponse {

    private Long applyId;

    private Long landId;

    private String companyEmail;

    private String companyName;

    private String message;

    private ApplyStatus status;

    public static ApplyResponse from(
            LandApply apply
    ) {

        return ApplyResponse.builder()
                .applyId(apply.getId())
                .landId(apply.getLand().getId())
                .companyEmail(apply.getCompany().getEmail())
                .companyName(apply.getCompany().getName())
                .message(apply.getMessage())
                .status(apply.getStatus())
                .build();
    }
}