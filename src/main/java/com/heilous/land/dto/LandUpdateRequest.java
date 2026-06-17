package com.heilous.land.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LandUpdateRequest {

    private String address;

    private Long desiredPrice;

    private String description;
}