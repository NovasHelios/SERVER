package com.heilous.bookmark.repository;

import com.heilous.bookmark.entity.Bookmark;
import com.heilous.bookmark.entity.BookmarkFolder;
import com.heilous.bookmark.entity.BookmarkTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserIdOrderByIdDesc(Long userId);

    List<Bookmark> findByFolderOrderByIdDesc(BookmarkFolder folder);

    List<Bookmark> findByTagsContainingOrderByIdDesc(BookmarkTag tag);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndUrl(Long userId, String url);
}
