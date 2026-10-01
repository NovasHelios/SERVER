package com.heilous.user.dto;

import com.heilous.user.entity.User;
import com.heilous.user.enums.UserPlan;
import com.heilous.user.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPlanResponse {

    private Long id;
    private String email;
    private String name;
    private UserRole role;
    private UserPlan plan;

    public static UserPlanResponse from(User user) {
        return UserPlanResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .plan(user.getPlan())
                .build();
    }
}
