package com.heilous.bookmark.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FolderRequest {

    @NotBlank
    private String name;
}
