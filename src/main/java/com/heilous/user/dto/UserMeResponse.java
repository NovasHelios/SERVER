package com.heilous.user.dto;

import com.heilous.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserMeResponse {

    private String email;
    private String name;
    private String phone;
    private UserRole role;
}