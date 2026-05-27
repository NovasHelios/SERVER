package com.heilous.bookmark.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TagRequest {

    @NotBlank
    private String name;
}
