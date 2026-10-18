package com.heilous.vworld.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NasaPowerResponse {

    @JsonProperty("properties")
    private Properties properties;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        @JsonProperty("parameter")
        private Map<String, Map<String, Double>> parameter;
    }

    /**
     * ALLSKY_SFC_SW_DWN 일별 값의 평균 반환.
     * 유효하지 않은 값(-999)은 제외.
     */
    public Double getAverageSolarIrradiance() {
        if (properties == null || properties.getParameter() == null) return null;
        Map<String, Double> daily = properties.getParameter().get("ALLSKY_SFC_SW_DWN");
        if (daily == null || daily.isEmpty()) return null;

        double sum = 0;
        int count = 0;
        for (Double v : daily.values()) {
            if (v != null && v > -998) {
                sum += v;
                count++;
            }
        }
        return count > 0 ? Math.round(sum / count * 1000.0) / 1000.0 : null;
    }
}
