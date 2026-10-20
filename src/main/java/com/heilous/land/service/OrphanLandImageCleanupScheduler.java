package com.heilous.land.service;

import com.heilous.common.service.ImageStorageService;
import com.heilous.land.repository.LandImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanLandImageCleanupScheduler {

    private static final String LAND_IMAGE_DIRECTORY = "lands";
    private static final Duration RETENTION_PERIOD = Duration.ofHours(24);

    private final LandImageRepository landImageRepository;
    private final ImageStorageService imageStorageService;

    /** 매일 한국 시간 새벽 3시에 DB에서 참조되지 않는 24시간 경과 파일을 정리한다. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void deleteOrphanLandImages() {
        Set<String> referencedFilenames = new HashSet<>(landImageRepository.findAllImagePaths());
        int deletedCount = imageStorageService.deleteUnreferencedFilesOlderThan(
                LAND_IMAGE_DIRECTORY, referencedFilenames, Instant.now().minus(RETENTION_PERIOD));

        if (deletedCount > 0) {
            log.info("참조되지 않는 토지 이미지 {}개를 삭제했습니다.", deletedCount);
        }
    }
}
