package com.heilous.land.entity;

import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Land {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner; // 등록한 토지소유자

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double area; // 면적 (평수 또는 m2)

    private Long desiredPrice; // 희망 매매/임대 가격

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private LandStatus status; // PENDING, APPROVED, REJECTED

    public enum LandStatus { PENDING, APPROVED, REJECTED }
}

