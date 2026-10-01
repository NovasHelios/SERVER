package com.heilous.user.dto;

import com.heilous.user.enums.UserPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePlanRequest {

    @NotNull
    private UserPlan plan;
}
