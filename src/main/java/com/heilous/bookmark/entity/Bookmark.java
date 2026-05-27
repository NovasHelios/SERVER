package com.heilous.bookmark.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private BookmarkFolder folder;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToMany
    @JoinTable(
            name = "bookmark_tag_map",
            joinColumns = @JoinColumn(name = "bookmark_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<BookmarkTag> tags = new ArrayList<>();

    public void update(String url, String title, String description, BookmarkFolder folder) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.folder = folder;
    }

    public void addTag(BookmarkTag tag) {
        this.tags.add(tag);
    }

    public void removeTag(BookmarkTag tag) {
        this.tags.remove(tag);
    }
}
