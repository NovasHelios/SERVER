package com.heilous.land.dto;

import com.heilous.land.entity.Land;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LandRegisterRequest {
    private String address;
    private Long desiredPrice;
    private String description;
    private Land.TransactionType transactionType;
}