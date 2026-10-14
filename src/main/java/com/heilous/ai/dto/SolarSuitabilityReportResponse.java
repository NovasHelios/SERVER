package com.heilous.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SolarSuitabilityReportResponse {
    private Long landId;
    private String address;
    private List<SolarCriterionReport> criteria;
}
