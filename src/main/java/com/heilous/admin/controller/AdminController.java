package com.heilous.admin.controller;

import com.heilous.common.dto.APIResponse;
import com.heilous.land.service.LandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@CrossOrigin
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LandService landService;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Operation(summary = "토지 등록 승인(ADMIN) — 관리자가 유저가 등록 신청한 토지를 최종 승인합니다.")
    @PatchMapping("/lands/{landId}/approve")
    public APIResponse<String> approveLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        landService.approveLand(landId, email);
        return APIResponse.ok("토지 승인 완료");
    }

    @Operation(summary = "토지 등록 거절(ADMIN) — 관리자가 유저가 등록 신청한 토지를 거절합니다.")
    @PatchMapping("/lands/{landId}/reject")
    public APIResponse<String> rejectLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        landService.rejectLand(landId, email);
        return APIResponse.ok("토지 거절 완료");
    }

    @Operation(summary = "토지 증명서 다운로드(ADMIN) — 관리자가 토지 검증을 위해 제출된 증명서 파일을 다운로드합니다.")
    @GetMapping("/lands/{landId}/document")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        String filename = landService.getDocumentPath(landId, email);
        try {
            Path filePath = Paths.get(uploadDir, "documents", filename);
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}