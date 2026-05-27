package com.heilous.bookmark.repository;

import com.heilous.bookmark.entity.BookmarkTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkTagRepository extends JpaRepository<BookmarkTag, Long> {

    List<BookmarkTag> findByUserIdOrderByIdDesc(Long userId);

    Optional<BookmarkTag> findByIdAndUserId(Long id, Long userId);
}
