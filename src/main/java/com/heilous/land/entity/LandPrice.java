package com.heilous.land.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 공시지가 이력 (land_prices 테이블)
 * VWorld getReferLandPriceWFS API 응답 기준
 */
@Entity
@Table(name = "land_prices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LandPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "land_id", nullable = false)
    private Land land;

    private String stdrYear;   // 공시기준연도 (pblntf_pclnd_stdr_year)
    private String stdrMt;     // 공시기준월   (pblntf_pclnd_stdr_mt)

    private Long pblntfPclnd;       // 당해 공시지가        (pblntf_pclnd)
    private Long pstyr1PblntfPclnd; // 전년도 공시지가      (pstyr_1_pblntf_pclnd)
    private Long pstyr2PblntfPclnd; // 2년 전 공시지가      (pstyr_2_pblntf_pclnd)
    private Long pstyr3PblntfPclnd; // 3년 전 공시지가      (pstyr_3_pblntf_pclnd)
    private Long pstyr4PblntfPclnd; // 4년 전 공시지가      (pstyr_4_pblntf_pclnd)
}
