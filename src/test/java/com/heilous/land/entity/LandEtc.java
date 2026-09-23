package com.heilous.land.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 토지_기타
 * prpos_area_dstrc_code_list 중 UQA~UQE(용도지역), UQF~UQP(용도지구) 를 제외한 나머지
 */
@Entity
@Table(name = "land_etc")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LandEtc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "land_id", nullable = false)
    private Land land;

    @Column(nullable = false, length = 30)
    private String code;    // 코드 (예: ZA0006)

    /** 1=포함, 2=저촉, 3=접함 */
    @Column(length = 1)
    private String cnflcAt;

    @Column(length = 10)
    private String cnflcAtNm;
}
