package com.heilous.land.dto;

import com.heilous.land.entity.Land;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LandResponse {

    private Long id;
    private String ownerEmail;

    private String address;
    private Double area;
    private Long desiredPrice;
    private String description;

    private String status;

    public static LandResponse from(Land land) {
        return LandResponse.builder()
                .id(land.getId())
                .ownerEmail(land.getOwner().getEmail())
                .address(land.getAddress())
                .area(land.getArea())
                .desiredPrice(land.getDesiredPrice())
                .description(land.getDescription())
                .status(land.getStatus().name())
                .build();
    }
}