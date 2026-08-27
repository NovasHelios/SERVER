package com.heilous.land.dto;

import com.heilous.land.entity.Land;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LandFilterRequest {

    private Land.LandStatus status; // nullable → null이면 전체 상태

    private Land.TransactionType transactionType;

    // 매매 가격 범위
    private Long saleMinPrice;
    private Long saleMaxPrice;

    // 임대 가격 범위
    private Long leaseMinPrice;
    private Long leaseMaxPrice;

    private Double minArea;
    private Double maxArea;

    private String sido;
    private String sigungu;
    private String eupmyeondong;
}
