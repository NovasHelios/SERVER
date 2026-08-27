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
    private String lcCode;
    private String lcCodeNm;
    private String lastUpdtDt;
    private String regstrSeCodeNm;
    private String cnrsPsnCo;
    private String pnu;
    private String ldCodeNm;
    private String regionSido;
    private String regionSigungu;
    private String regionEupmyeondong;
    private Long desiredPrice;
    private String description;

    private String status;
    private String transactionType;
    private String landImagePath;
    private String documentPath;
    private String x;
    private String y;

    public static LandResponse from(Land land) {
        return LandResponse.builder()
                .id(land.getId())
                .ownerEmail(land.getOwner().getEmail())
                .address(land.getAddress())
                .area(land.getArea())
                .lcCode(land.getLcCode())
                .lcCodeNm(land.getLcCodeNm())
                .lastUpdtDt(land.getLastUpdtDt())
                .regstrSeCodeNm(land.getRegstrSeCodeNm())
                .cnrsPsnCo(land.getCnrsPsnCo())
                .pnu(land.getPnu())
                .ldCodeNm(land.getLdCodeNm())
                .regionSido(land.getRegionSido())
                .regionSigungu(land.getRegionSigungu())
                .regionEupmyeondong(land.getRegionEupmyeondong())
                .desiredPrice(land.getDesiredPrice())
                .description(land.getDescription())
                .status(land.getStatus() != null ? land.getStatus().name() : null)
                .transactionType(land.getTransactionType() != null ? land.getTransactionType().name() : null)
                .landImagePath(land.getLandImagePath())
                .documentPath(land.getDocumentPath())
                .x(land.getX())
                .y(land.getY())
                .build();
    }
}