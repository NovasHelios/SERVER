package com.heilous.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SolarCriterionReport {
    private String criterion;
    private Integer score;
    private Integer maxScore;
    private String explanation;
}
