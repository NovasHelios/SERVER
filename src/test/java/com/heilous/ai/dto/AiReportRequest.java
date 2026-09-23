package com.heilous.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiReportRequest {

    @NotNull(message = "토지 ID는 필수입니다.")
    private Long landId;
}
