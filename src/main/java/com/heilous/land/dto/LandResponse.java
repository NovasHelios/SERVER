package com.heilous.land.dto;

import com.heilous.land.entity.Land;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LandResponse {

    private Long id;
    private String address;
    private Double area;
    private String pnu;
    private Long desiredPrice;
    private String transactionType;
    private List<String> landImagePaths;
    private Double x;
    private Double y;
    private String lcCodeNm;
    private String regstrSeCodeNm;

    public static LandResponse from(Land land) {
        return LandResponse.builder()
                .id(land.getId())
                .address(land.getAddress())
                .area(land.getArea())
                .pnu(land.getPnu())
                .desiredPrice(land.getDesiredPrice())
                .transactionType(land.getTransactionType() != null ? land.getTransactionType().name() : null)
                .landImagePaths(land.getLandImages().stream().map(img -> img.getImagePath()).toList())
                .x(land.getX())
                .y(land.getY())
                .lcCodeNm(land.getLcCodeNm())
                .regstrSeCodeNm(land.getRegstrSeCodeNm())
                .build();
    }
}