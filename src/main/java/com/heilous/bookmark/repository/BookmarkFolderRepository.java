package com.heilous.bookmark.repository;

import com.heilous.bookmark.entity.BookmarkFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkFolderRepository extends JpaRepository<BookmarkFolder, Long> {

    List<BookmarkFolder> findByUserIdOrderByIdDesc(Long userId);

    Optional<BookmarkFolder> findByIdAndUserId(Long id, Long userId);
}
