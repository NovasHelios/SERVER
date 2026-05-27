package com.heilous.bookmark.dto;

import com.heilous.bookmark.entity.BookmarkTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {

    private Long id;
    private String name;

    public static TagResponse from(BookmarkTag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .build();
    }
}
