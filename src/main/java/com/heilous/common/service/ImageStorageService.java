package com.heilous.common.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class ImageStorageService {

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png"
    );

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    /**
     * MultipartFile을 UUID 기반 고유 파일명으로 저장하고 파일명을 반환합니다.
     * @param file    업로드된 파일
     * @param subDir  하위 디렉토리 (예: "lands", "profiles")
     * @return 저장된 고유 파일명 (예: "550e8400-e29b-41d4-a716-446655440000.jpg")
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new CustomException(GlobalErrorCode.INVALID_IMAGE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFilename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Paths.get(uploadDir, subDir);
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(uniqueFilename));
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", uniqueFilename, e);
            throw new CustomException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }

        return uniqueFilename;
    }

    /**
     * 증명서 파일(pdf/jpg/png)을 저장하고 파일명을 반환합니다.
     */
    public String storeDocument(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
            throw new CustomException(GlobalErrorCode.INVALID_DOCUMENT_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFilename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Paths.get(uploadDir, subDir);
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(uniqueFilename));
        } catch (IOException e) {
            log.error("문서 저장 실패: {}", uniqueFilename, e);
            throw new CustomException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }

        return uniqueFilename;
    }

    /**
     * 기존 파일을 삭제합니다. 없어도 예외를 던지지 않습니다.
     */
    public void delete(String filename, String subDir) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path file = Paths.get(uploadDir, subDir, filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}/{}", subDir, filename);
        }
    }

    /**
     * DB에서 참조되지 않고 기준 시각 이전에 업로드된 파일만 삭제합니다.
     * 참조되지 않는 파일은 DB 행이 없으므로 파일의 마지막 수정 시각을 업로드 시각으로 사용합니다.
     */
    public int deleteUnreferencedFilesOlderThan(String subDir, Set<String> referencedFilenames,
                                                 Instant cutoff) {
        Path targetDir = Paths.get(uploadDir, subDir);
        if (!Files.isDirectory(targetDir)) return 0;

        int deletedCount = 0;
        try (Stream<Path> files = Files.list(targetDir)) {
            Iterator<Path> iterator = files.iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                String filename = file.getFileName().toString();
                if (referencedFilenames.contains(filename)) continue;
                if (Files.getLastModifiedTime(file).toInstant().isAfter(cutoff)) continue;

                if (Files.deleteIfExists(file)) {
                    deletedCount++;
                    log.info("참조되지 않는 토지 이미지 파일 삭제: {}", filename);
                }
            }
        } catch (IOException e) {
            log.warn("참조되지 않는 파일 정리 실패 ({}/): {}", subDir, e.getMessage());
        }
        return deletedCount;
    }
}
