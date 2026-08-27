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

    @Operation(
            summary = "토지 찜 등록",
            description = "COMPANY 사용자가 관심 있는 토지를 찜 목록에 추가합니다. 이미 찜한 토지를 다시 요청하면 오류를 반환합니다."
    )
    @PostMapping("/{landId}")
    public APIResponse<String> addWish(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        wishService.addWish(landId, email);
        return APIResponse.ok("찜 목록에 추가되었습니다.");
    }

    @Operation(
            summary = "토지 찜 취소",
            description = "찜 목록에서 특정 토지를 제거합니다. 본인이 찜한 토지만 취소할 수 있습니다."
    )
    @DeleteMapping("/{landId}")
    public APIResponse<String> removeWish(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        wishService.removeWish(landId, email);
        return APIResponse.ok("찜 목록에서 제거되었습니다.");
    }

    @Operation(
            summary = "내 찜 목록 조회",
            description = "로그인한 사용자가 찜한 토지 목록을 조회합니다. 각 항목에는 토지 기본 정보가 포함됩니다."
    )
    @GetMapping
    public APIResponse<List<WishResponse>> getMyWishes(
            @AuthenticationPrincipal String email
    ) {
        return APIResponse.ok(wishService.getMyWishes(email));
    }
}
