package com.heilous.bookmark.controller;

import com.heilous.bookmark.dto.*;
import com.heilous.bookmark.service.BookmarkService;
import com.heilous.common.dto.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookmark", description = "북마크 API")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // ── 북마크 ──────────────────────────────────────────

    @Operation(summary = "북마크 추가")
    @PostMapping
    public APIResponse<BookmarkResponse> addBookmark(
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.addBookmark(request, email));
    }

    @Operation(summary = "북마크 수정")
    @PutMapping("/{bookmarkId}")
    public APIResponse<BookmarkResponse> updateBookmark(
            @PathVariable Long bookmarkId,
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.updateBookmark(bookmarkId, request, email));
    }

    @Operation(summary = "북마크 삭제")
    @DeleteMapping("/{bookmarkId}")
    public APIResponse<String> deleteBookmark(
            @PathVariable Long bookmarkId,
            @AuthenticationPrincipal String email
    ) {
        bookmarkService.deleteBookmark(bookmarkId, email);
        return APIResponse.ok("북마크가 삭제되었습니다.");
    }

    @Operation(summary = "내 북마크 전체 조회")
    @GetMapping
    public APIResponse<List<BookmarkResponse>> getMyBookmarks(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.getMyBookmarks(email));
    }

    @Operation(summary = "폴더별 북마크 조회")
    @GetMapping("/folder/{folderId}")
    public APIResponse<List<BookmarkResponse>> getBookmarksByFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.getBookmarksByFolder(folderId, email));
    }

    @Operation(summary = "태그별 북마크 조회")
    @GetMapping("/tag/{tagId}")
    public APIResponse<List<BookmarkResponse>> getBookmarksByTag(
            @PathVariable Long tagId,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.getBookmarksByTag(tagId, email));
    }

    // ── 폴더 ──────────────────────────────────────────

    @Operation(summary = "폴더 생성")
    @PostMapping("/folders")
    public APIResponse<FolderResponse> createFolder(
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.createFolder(request, email));
    }

    @Operation(summary = "폴더 수정")
    @PutMapping("/folders/{folderId}")
    public APIResponse<FolderResponse> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.updateFolder(folderId, request, email));
    }

    @Operation(summary = "폴더 삭제")
    @DeleteMapping("/folders/{folderId}")
    public APIResponse<String> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal String email
    ) {
        bookmarkService.deleteFolder(folderId, email);
        return APIResponse.ok("폴더가 삭제되었습니다.");
    }

    @Operation(summary = "내 폴더 목록 조회")
    @GetMapping("/folders")
    public APIResponse<List<FolderResponse>> getMyFolders(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.getMyFolders(email));
    }

    // ── 태그 ──────────────────────────────────────────

    @Operation(summary = "태그 생성")
    @PostMapping("/tags")
    public APIResponse<TagResponse> createTag(
            @Valid @RequestBody TagRequest request,
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.createTag(request, email));
    }

    @Operation(summary = "태그 삭제")
    @DeleteMapping("/tags/{tagId}")
    public APIResponse<String> deleteTag(
            @PathVariable Long tagId,
            @AuthenticationPrincipal String email
    ) {
        bookmarkService.deleteTag(tagId, email);
        return APIResponse.ok("태그가 삭제되었습니다.");
    }

    @Operation(summary = "내 태그 목록 조회")
    @GetMapping("/tags")
    public APIResponse<List<TagResponse>> getMyTags(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(bookmarkService.getMyTags(email));
    }
}
