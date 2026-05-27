package com.heilous.bookmark.service;

import com.heilous.bookmark.dto.*;
import com.heilous.bookmark.entity.Bookmark;
import com.heilous.bookmark.entity.BookmarkFolder;
import com.heilous.bookmark.entity.BookmarkTag;
import com.heilous.bookmark.repository.BookmarkFolderRepository;
import com.heilous.bookmark.repository.BookmarkRepository;
import com.heilous.bookmark.repository.BookmarkTagRepository;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.user.entity.User;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkFolderRepository folderRepository;
    private final BookmarkTagRepository tagRepository;
    private final UserRepository userRepository;

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }

    // ── 북마크 ──────────────────────────────────────────

    @Transactional
    public BookmarkResponse addBookmark(BookmarkRequest request, String email) {
        User user = getUser(email);

        if (bookmarkRepository.existsByUserIdAndUrl(user.getId(), request.getUrl())) {
            throw new CustomException(GlobalErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        BookmarkFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdAndUserId(request.getFolderId(), user.getId())
                    .orElseThrow(() -> new CustomException(GlobalErrorCode.FOLDER_NOT_FOUND));
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .folder(folder)
                .url(request.getUrl())
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        if (request.getTagIds() != null) {
            for (Long tagId : request.getTagIds()) {
                BookmarkTag tag = tagRepository.findByIdAndUserId(tagId, user.getId())
                        .orElseThrow(() -> new CustomException(GlobalErrorCode.TAG_NOT_FOUND));
                bookmark.addTag(tag);
            }
        }

        return BookmarkResponse.from(bookmarkRepository.save(bookmark));
    }

    @Transactional
    public BookmarkResponse updateBookmark(Long bookmarkId, BookmarkRequest request, String email) {
        User user = getUser(email);

        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.BOOKMARK_NOT_FOUND));

        BookmarkFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdAndUserId(request.getFolderId(), user.getId())
                    .orElseThrow(() -> new CustomException(GlobalErrorCode.FOLDER_NOT_FOUND));
        }

        bookmark.update(request.getUrl(), request.getTitle(), request.getDescription(), folder);

        if (request.getTagIds() != null) {
            bookmark.getTags().clear();
            for (Long tagId : request.getTagIds()) {
                BookmarkTag tag = tagRepository.findByIdAndUserId(tagId, user.getId())
                        .orElseThrow(() -> new CustomException(GlobalErrorCode.TAG_NOT_FOUND));
                bookmark.addTag(tag);
            }
        }

        return BookmarkResponse.from(bookmark);
    }

    @Transactional
    public void deleteBookmark(Long bookmarkId, String email) {
        User user = getUser(email);

        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getMyBookmarks(String email) {
        User user = getUser(email);
        return bookmarkRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream().map(BookmarkResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarksByFolder(Long folderId, String email) {
        User user = getUser(email);

        BookmarkFolder folder = folderRepository.findByIdAndUserId(folderId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.FOLDER_NOT_FOUND));

        return bookmarkRepository.findByFolderOrderByIdDesc(folder)
                .stream().map(BookmarkResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarksByTag(Long tagId, String email) {
        User user = getUser(email);

        BookmarkTag tag = tagRepository.findByIdAndUserId(tagId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.TAG_NOT_FOUND));

        return bookmarkRepository.findByTagsContainingOrderByIdDesc(tag)
                .stream().map(BookmarkResponse::from).toList();
    }

    // ── 폴더 ──────────────────────────────────────────

    @Transactional
    public FolderResponse createFolder(FolderRequest request, String email) {
        User user = getUser(email);

        BookmarkFolder folder = BookmarkFolder.builder()
                .user(user)
                .name(request.getName())
                .build();

        return FolderResponse.from(folderRepository.save(folder));
    }

    @Transactional
    public FolderResponse updateFolder(Long folderId, FolderRequest request, String email) {
        User user = getUser(email);

        BookmarkFolder folder = folderRepository.findByIdAndUserId(folderId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.FOLDER_NOT_FOUND));

        folder.updateName(request.getName());
        return FolderResponse.from(folder);
    }

    @Transactional
    public void deleteFolder(Long folderId, String email) {
        User user = getUser(email);

        BookmarkFolder folder = folderRepository.findByIdAndUserId(folderId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.FOLDER_NOT_FOUND));

        folderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getMyFolders(String email) {
        User user = getUser(email);
        return folderRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream().map(FolderResponse::from).toList();
    }

    // ── 태그 ──────────────────────────────────────────

    @Transactional
    public TagResponse createTag(TagRequest request, String email) {
        User user = getUser(email);

        BookmarkTag tag = BookmarkTag.builder()
                .user(user)
                .name(request.getName())
                .build();

        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long tagId, String email) {
        User user = getUser(email);

        BookmarkTag tag = tagRepository.findByIdAndUserId(tagId, user.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.TAG_NOT_FOUND));

        tagRepository.delete(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getMyTags(String email) {
        User user = getUser(email);
        return tagRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream().map(TagResponse::from).toList();
    }
}
