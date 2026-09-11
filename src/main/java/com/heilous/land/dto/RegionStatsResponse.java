package com.heilous.land.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RegionStatsResponse {

    private String sido;
    private List<SigunguStats> sigungus;

    @Getter
    @Builder
    public static class SigunguStats {
        private String sigungu;
        private long saleCount;
        private long leaseCount;
        private long businessHopeCount;
    }
}
