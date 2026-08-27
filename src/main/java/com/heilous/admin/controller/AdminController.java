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

    @Operation(
            summary = "토지 등록 승인 (관리자)",
            description = "관리자(ADMIN)가 USER가 신청한 토지 등록을 최종 승인합니다. 승인된 토지는 상태가 PENDING → APPROVED로 변경되어 목록에 공개됩니다."
    )
    @PatchMapping("/lands/{landId}/approve")
    public APIResponse<String> approveLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        landService.approveLand(landId, email);
        return APIResponse.ok("토지 승인 완료");
    }

    @Operation(
            summary = "토지 등록 거절 (관리자)",
            description = "관리자(ADMIN)가 USER가 신청한 토지 등록을 거절합니다. 거절된 토지는 상태가 PENDING → REJECTED로 변경됩니다."
    )
    @PatchMapping("/lands/{landId}/reject")
    public APIResponse<String> rejectLand(
            @PathVariable Long landId,
            @AuthenticationPrincipal String email
    ) {
        landService.rejectLand(landId, email);
        return APIResponse.ok("토지 거절 완료");
    }

    @Operation(
            summary = "토지 증명서 다운로드 (관리자)",
            description = "관리자(ADMIN)가 토지 등록 검증을 위해 유저가 제출한 증명서 파일을 다운로드합니다. 파일이 존재하지 않는 경우 404를 반환합니다."
    )
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