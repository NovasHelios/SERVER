package com.heilous.user.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.user.enums.UserPlan;
import com.heilous.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserPlan plan = UserPlan.FREE;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(nullable = false)
    private boolean isActive;

    @Column
    private String provider;

    private String profileImagePath;

    public void deactivate() {
        this.isActive = false;
    }

    public void updateInfo(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void updateProfileImage(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public void changePlan(UserPlan plan) {
        this.plan = plan;
    }
}
