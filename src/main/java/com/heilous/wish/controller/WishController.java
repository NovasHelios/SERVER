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

@Tag(name = "Wish", description = "찜하기 API")
@CrossOrigin
@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;

    @Operation(summary = "찜 추가")
    @PostMapping("/{landId}")
    public APIResponse<String> addWish(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        wishService.addWish(landId, email);
        return APIResponse.ok("찜 목록에 추가되었습니다.");
    }

    @Operation(summary = "찜 취소")
    @DeleteMapping("/{landId}")
    public APIResponse<String> removeWish(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        wishService.removeWish(landId, email);
        return APIResponse.ok("찜 목록에서 제거되었습니다.");
    }

    @Operation(summary = "내 찜 목록 조회")
    @GetMapping
    public APIResponse<List<WishResponse>> getMyWishes(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(wishService.getMyWishes(email));
    }
}
