package com.heilous.wish.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.wish.dto.WishResponse;
import com.heilous.wish.service.WishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Wish", description = "찜하기 API(COMPANY)")
@CrossOrigin
@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;

    @Operation(summary = "토지 찜하기(관심 등록) — 특정 토지를 내 관심 목록에 추가합니다.")
    @PostMapping("/{landId}")
    public APIResponse<String> addWish(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        wishService.addWish(landId, email);
        return APIResponse.ok("찜 목록에 추가되었습니다.");
    }

    @Operation(summary = "토지 찜 취소 — 관심 목록에서 해당 토지를 제거합니다.")
    @DeleteMapping("/{landId}")
    public APIResponse<String> removeWish(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        wishService.removeWish(landId, email);
        return APIResponse.ok("찜 목록에서 제거되었습니다.");
    }

    @Operation(summary = "내 찜 목록 조회 — 본인이 찜한 토지 목록을 조회합니다.")
    @GetMapping
    public APIResponse<List<WishResponse>> getMyWishes(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(wishService.getMyWishes(email));
    }
}
