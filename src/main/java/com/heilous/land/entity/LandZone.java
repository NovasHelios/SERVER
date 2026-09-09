package com.heilous.land.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 토지_용도지구
 * prpos_area_dstrc_code_list 중 UQF ~ UQP 로 시작하는 항목
 */
@Entity
@Table(name = "land_zones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LandZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "land_id", nullable = false)
    private Land land;

    @Column(nullable = false, length = 20)
    private String code;    // 코드 (예: UQG100)

    /** 1=포함, 2=저촉, 3=접함 */
    @Column(length = 1)
    private String cnflcAt;

    @Column(length = 10)
    private String cnflcAtNm; // 포함/저촉/접함
}
