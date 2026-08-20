package com.heilous.land.dto;

import com.heilous.land.entity.Land;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LandUpdateRequest {

    private String address;

    private Long desiredPrice;

    private String description;

    private Land.TransactionType transactionType;
}