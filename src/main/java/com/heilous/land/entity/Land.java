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

    @Column(nullable = false)
    private Double area;

    private Long desiredPrice;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private LandStatus status;

    public enum LandStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    // 수정
    public void updateLand(
            String address,
            Double area,
            Long desiredPrice,
            String description
    ) {

        this.address = address;
        this.area = area;
        this.desiredPrice = desiredPrice;
        this.description = description;
    }

    // 상태 변경
    public void changeStatus(LandStatus status) {
        this.status = status;
    }
}