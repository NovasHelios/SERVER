package com.heilous.wish.dto;

import com.heilous.wish.entity.Wish;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WishResponse {

    private Long wishId;
    private Long landId;
    private String address;
    private Double area;
    private Long desiredPrice;
    private String status;
    private LocalDateTime wishedAt;

    public static WishResponse from(Wish wish) {
        return WishResponse.builder()
                .wishId(wish.getId())
                .landId(wish.getLand().getId())
                .address(wish.getLand().getAddress())
                .area(wish.getLand().getArea())
                .desiredPrice(wish.getLand().getDesiredPrice())
                .status(wish.getLand().getStatus().name())
                .wishedAt(wish.getCreatedAt())
                .build();
    }
}
