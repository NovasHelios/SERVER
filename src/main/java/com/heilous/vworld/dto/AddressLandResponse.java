package com.heilous.vworld.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressLandResponse {

    private String pnu;           // 생성된 PNU
    private String addressName;   // 검색된 전체 주소
    private Double x;             // 경도
    private Double y;             // 위도
    private String zoneNo;        // 우편번호 (도로명 주소 기준)
    private String buildingName;  // 건물명
    private VWorldLandResponse landInfo; // 토지 임야 정보
}
