package com.heilous.bookmark.dto;

import com.heilous.bookmark.entity.BookmarkFolder;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FolderResponse {

    private Long id;
    private String name;
    private LocalDateTime createdAt;

    public static FolderResponse from(BookmarkFolder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
