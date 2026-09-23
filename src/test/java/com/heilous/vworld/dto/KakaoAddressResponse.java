package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoAddressResponse {

    private Meta meta;
    private List<Document> documents;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("total_count")
        private Integer totalCount;
        @JsonProperty("pageable_count")
        private Integer pageableCount;
        @JsonProperty("is_end")
        private Boolean isEnd;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("address_name")
        private String addressName;
        @JsonProperty("address_type")
        private String addressType;
        private String x; // 경도(Longitude)
        private String y; // 위도(Latitude)
        private Address address;
        @JsonProperty("road_address")
        private RoadAddress roadAddress;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        @JsonProperty("address_name")
        private String addressName;
        @JsonProperty("region_1depth_name")
        private String region1DepthName;
        @JsonProperty("region_2depth_name")
        private String region2DepthName;
        @JsonProperty("region_3depth_name")
        private String region3DepthName;
        @JsonProperty("region_3depth_h_name")
        private String region3DepthHName;
        @JsonProperty("h_code")
        private String hCode;
        @JsonProperty("b_code")
        private String bCode;
        @JsonProperty("mountain_yn")
        private String mountainYn;
        @JsonProperty("main_address_no")
        private String mainAddressNo;
        @JsonProperty("sub_address_no")
        private String subAddressNo;
        private String x;
        private String y;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoadAddress {
        @JsonProperty("address_name")
        private String addressName;
        @JsonProperty("region_1depth_name")
        private String region1DepthName;
        @JsonProperty("region_2depth_name")
        private String region2DepthName;
        @JsonProperty("region_3depth_name")
        private String region3DepthName;
        @JsonProperty("road_name")
        private String roadName;
        @JsonProperty("underground_yn")
        private String undergroundYn;
        @JsonProperty("main_building_no")
        private String mainBuildingNo;
        @JsonProperty("sub_building_no")
        private String subBuildingNo;
        @JsonProperty("building_name")
        private String buildingName;
        @JsonProperty("zone_no")
        private String zoneNo;
        private String x;
        private String y;
    }
}
