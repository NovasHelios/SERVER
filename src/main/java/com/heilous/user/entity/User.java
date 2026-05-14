package com.heilous.user.entity;

import com.heilous.common.entity.BaseEntity;
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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(nullable = false)
    private boolean isActive;

    // 계정 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 프로필 수정
    public void updateInfo(
            String name,
            String phone
    ) {
        this.name = name;
        this.phone = phone;
    }

    // 비밀번호 변경
    public void changePassword(
            String password
    ) {
        this.password = password;
    }
}