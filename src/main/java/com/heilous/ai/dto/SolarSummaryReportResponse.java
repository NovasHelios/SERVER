package com.heilous.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SolarSummaryReportResponse {
    private Long landId;
    private String address;
    private Integer totalScore;
    private String grade;
    private boolean disqualified;
    private String report;
}
