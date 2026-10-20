package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LandCharacteristicsWfsResponse {

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
        /** 도로접면코드 (1=광대한면, 2=광대소각, 3=소한면, 4=소소각, 5=맹지 등) */
        @JsonProperty("road_side_code")
        private String roadSideCode;
    }

    public String getRoadSideCode() {
        if (features == null || features.isEmpty()) return null;
        Feature f = features.get(0);
        if (f.getProperties() == null) return null;
        return f.getProperties().getRoadSideCode();
    }
}
