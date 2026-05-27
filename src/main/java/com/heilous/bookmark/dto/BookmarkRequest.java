package com.heilous.bookmark.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class BookmarkRequest {

    @NotBlank
    private String url;

    @NotBlank
    private String title;

    private String description;

    private Long folderId;

    private List<Long> tagIds;
}
