package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VWorldLandResponse {

    private LandData ladfrlVOList;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LandData {
        private List<LandInfo> ladfrlVOList;
        private Integer totalCount;
        private Integer pageNo;
        private Integer numOfRows;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LandInfo {

        private String pnu; // 고유번호

        @JsonProperty("ldCode")
        private String ldCode; // 법정동코드

        @JsonProperty("ldCodeNm")
        private String ldCodeNm; // 법정동명

        @JsonProperty("mnnmSlno")
        private String mnnmSlno; // 지번

        @JsonProperty("regstrSeCode")
        private String regstrSeCode; // 대장구분코드

        @JsonProperty("regstrSeCodeNm")
        private String regstrSeCodeNm; // 대장구분명

        @JsonProperty("lndcgrCode")
        private String lndcgrCode; // 지목코드

        @JsonProperty("lndcgrCodeNm")
        private String lndcgrCodeNm; // 지목명

        @JsonProperty("lndpclAr")
        private String lndpclAr; // 면적(㎡)

        @JsonProperty("posesnSeCode")
        private String posesnSeCode; // 소유구분코드

        @JsonProperty("posesnSeCodeNm")
        private String posesnSeCodeNm; // 소유구분명

        @JsonProperty("cnrsPsnCo")
        private String cnrsPsnCo; // 소유(공유)인수(명)

        @JsonProperty("ladFrtlSc")
        private String ladFrtlSc; // 축척구분코드

        @JsonProperty("ladFrtlScNm")
        private String ladFrtlScNm; // 축척구분명

        @JsonProperty("lastUpdtDt")
        private String lastUpdtDt; // 데이터기준일자
    }
}
