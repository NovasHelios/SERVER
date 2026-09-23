package com.heilous.land.dto;

import com.heilous.land.entity.Land;
import com.heilous.land.entity.LandEtc;
import com.heilous.land.entity.LandZone;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LandDetailResponse {

    private Long id;
    private String ownerEmail;

    // 기본 토지 정보
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

    // 거래 정보
    private Long desiredPrice;
    private Double desiredArea;
    private String description;
    private String status;
    private String transactionType;

    // 좌표
    private Double x;
    private Double y;

    // 용도지역
    private String prposAreaCode;
    private String prposAreaCnflcAt;
    private String prposAreaCnflcAtNm;

    // 용도지구
    private List<ZoneItem> landZones;

    // 기타
    private List<EtcItem> landEtcs;

    // 이미지/문서
    private List<String> landImagePaths;
    private String documentPath;

    @Getter
    @Builder
    public static class ZoneItem {
        private String code;
        private String cnflcAt;
        private String cnflcAtNm;

        public static ZoneItem from(LandZone z) {
            return ZoneItem.builder()
                    .code(z.getCode())
                    .cnflcAt(z.getCnflcAt())
                    .cnflcAtNm(z.getCnflcAtNm())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EtcItem {
        private String code;
        private String cnflcAt;
        private String cnflcAtNm;

        public static EtcItem from(LandEtc e) {
            return EtcItem.builder()
                    .code(e.getCode())
                    .cnflcAt(e.getCnflcAt())
                    .cnflcAtNm(e.getCnflcAtNm())
                    .build();
        }
    }

    public static LandDetailResponse from(Land land) {
        return LandDetailResponse.builder()
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
                .desiredArea(land.getDesiredArea())
                .description(land.getDescription())
                .status(land.getStatus() != null ? land.getStatus().name() : null)
                .transactionType(land.getTransactionType() != null ? land.getTransactionType().name() : null)
                .x(land.getX())
                .y(land.getY())
                .prposAreaCode(land.getPrposAreaCode())
                .prposAreaCnflcAt(land.getPrposAreaCnflcAt())
                .prposAreaCnflcAtNm(land.getPrposAreaCnflcAtNm())
                .landZones(land.getLandZones().stream().map(ZoneItem::from).toList())
                .landEtcs(land.getLandEtcs().stream().map(EtcItem::from).toList())
                .landImagePaths(land.getLandImages().stream().map(img -> img.getImagePath()).toList())
                .documentPath(land.getDocumentPath())
                .build();
    }
}
