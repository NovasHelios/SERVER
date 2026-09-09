package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VWorldWfsResponse {

    @JsonProperty("features")
    private List<Feature> features;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {

        @JsonProperty("geometry")
        private Geometry geometry;

        @JsonProperty("properties")
        private Properties properties;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        @JsonProperty("coordinates")
        private Object coordinates; // MultiPolygon 좌표 배열 → JSON으로 저장
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {

        @JsonProperty("pnu")
        private String pnu;

        /** 용도지역/지구/기타 코드 목록 (쉼표 구분) */
        @JsonProperty("prpos_area_dstrc_code_list")
        private String prposAreaDstrcCodeList;

        /** 용도지역/지구/기타 명칭 목록 (쉼표 구분) */
        @JsonProperty("prpos_area_dstrc_nm_list")
        private String prposAreaDstrcNmList;

        /** 저촉 여부 코드 목록 (쉼표 구분) */
        @JsonProperty("cnflc_at_list")
        private String cnflcAtList;

        /** 저촉 여부 명칭 목록 (쉼표 구분) */
        @JsonProperty("cnflc_at_nm_list")
        private String cnflcAtNmList;
    }
}
