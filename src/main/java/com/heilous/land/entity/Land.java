package com.heilous.land.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Land extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String address;

    private Double area;

    private String lcCode;    // 지목코드

    private String lcCodeNm;  // 지목명

    private Long desiredPrice;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private LandStatus status;

    private String landImagePath; // 토지 이미지 파일명

    public enum LandStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public void updateLand(String address, Double area, String lcCode, String lcCodeNm, Long desiredPrice, String description) {
        this.address = address;
        this.area = area;
        this.lcCode = lcCode;
        this.lcCodeNm = lcCodeNm;
        this.desiredPrice = desiredPrice;
        this.description = description;
    }

    public void changeStatus(LandStatus status) {
        this.status = status;
    }

    public void updateImagePath(String landImagePath) {
        this.landImagePath = landImagePath;
    }

}