package com.heilous.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiReportResponse {

    private Long landId;
    private String address;
    private String report;
}
