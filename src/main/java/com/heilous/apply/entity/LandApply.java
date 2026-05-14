package com.heilous.apply.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.land.entity.Land;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LandApply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청한 업체
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private User company;

    // 신청 대상 토지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "land_id")
    private Land land;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private ApplyStatus status;

    public void changeStatus(
            ApplyStatus status
    ) {
        this.status = status;
    }
}