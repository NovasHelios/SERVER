package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * VWorld getPossessionAttr API 응답 DTO
 * https://api.vworld.kr/ned/data/getPossessionAttr
 * 토지대장 속성정보 (공시지가 포함)
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PossessionAttrResponse {

    @JsonProperty("possessionAttr")
    private PossessionAttr possessionAttr;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PossessionAttr {

        @JsonProperty("item")
        private List<Item> item;

        @JsonProperty("numOfRows")
        private String numOfRows;

        @JsonProperty("pageNo")
        private String pageNo;

        @JsonProperty("totalCount")
        private String totalCount;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        /** 고유번호 */
        @JsonProperty("pnu")
        private String pnu;

        /** 법정동코드 */
        @JsonProperty("ldCode")
        private String ldCode;

        /** 법정동명 */
        @JsonProperty("ldCodeNm")
        private String ldCodeNm;

        /** 대장구분명 */
        @JsonProperty("regstrSeCodeNm")
        private String regstrSeCodeNm;

        /** 지번 */
        @JsonProperty("mnnmSlno")
        private String mnnmSlno;

        /** 지목코드 */
        @JsonProperty("lndcgrCode")
        private String lndcgrCode;

        /** 지목명 */
        @JsonProperty("lndcgrCodeNm")
        private String lndcgrCodeNm;

        /** 토지면적(㎡) */
        @JsonProperty("lndpclAr")
        private String lndpclAr;

        /** 공시지가(원/㎡) */
        @JsonProperty("pblntfPclnd")
        private String pblntfPclnd;

        /** 소유구분명 */
        @JsonProperty("posesnSeCodeNm")
        private String posesnSeCodeNm;

        /** 기준연월 (예: 2023-01) */
        @JsonProperty("stdrYm")
        private String stdrYm;

        /** 데이터기준일자 */
        @JsonProperty("lastUpdtDt")
        private String lastUpdtDt;
    }
}
