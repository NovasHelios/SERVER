package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LandPriceWfsResponse {

    @JsonProperty("features")
    private List<Feature> features;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        @JsonProperty("properties")
        private Properties properties;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {

        @JsonProperty("pblntf_pclnd_stdr_year")
        private String pblntfPclndStdrYear;

        @JsonProperty("pblntf_pclnd_stdr_mt")
        private String pblntfPclndStdrMt;

        @JsonProperty("pblntf_pclnd")
        private Long pblntfPclnd;

        @JsonProperty("pstyr_1_pblntf_pclnd")
        private Long pstyr1PblntfPclnd;

        @JsonProperty("pstyr_2_pblntf_pclnd")
        private Long pstyr2PblntfPclnd;

        @JsonProperty("pstyr_3_pblntf_pclnd")
        private Long pstyr3PblntfPclnd;

        @JsonProperty("pstyr_4_pblntf_pclnd")
        private Long pstyr4PblntfPclnd;

        @JsonProperty("road_side_code")
        private String roadSideCode;
    }
}
