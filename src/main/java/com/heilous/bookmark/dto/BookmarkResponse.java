package com.heilous.bookmark.dto;

import com.heilous.bookmark.entity.Bookmark;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BookmarkResponse {

    private Long id;
    private String url;
    private String title;
    private String description;
    private Long folderId;
    private String folderName;
    private List<TagResponse> tags;
    private LocalDateTime createdAt;

    public static BookmarkResponse from(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .url(bookmark.getUrl())
                .title(bookmark.getTitle())
                .description(bookmark.getDescription())
                .folderId(bookmark.getFolder() != null ? bookmark.getFolder().getId() : null)
                .folderName(bookmark.getFolder() != null ? bookmark.getFolder().getName() : null)
                .tags(bookmark.getTags().stream().map(TagResponse::from).toList())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}
