package com.heilous.user.entity;

import com.heilous.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

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

    // --- 여기에 넣으시면 됩니다 ---

    // 계정 비활성화 메서드 (Soft Delete용)
    public void deactivate() {
        this.isActive = false;
    }

    // 정보 업데이트 메서드 (프로필 수정용)
    public void updateInfo(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}