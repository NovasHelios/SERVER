package com.heilous.land.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "land_images", indexes = {
        @Index(name = "idx_land_images_land_id", columnList = "land_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LandImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "land_id", nullable = false)
    private Land land;

    @Column(nullable = false)
    private String imagePath;
}
