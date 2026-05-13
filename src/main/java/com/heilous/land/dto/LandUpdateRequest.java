package com.heilous.land.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LandUpdateRequest {

    private String address;

    private Double area;

    private Long desiredPrice;

    private String description;
}