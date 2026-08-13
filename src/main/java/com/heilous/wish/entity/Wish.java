package com.heilous.wish.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.land.entity.Land;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "land_id"}),
        indexes = @Index(name = "idx_wishes_user_id_id", columnList = "user_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Wish extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "land_id", nullable = false)
    private Land land;
}
