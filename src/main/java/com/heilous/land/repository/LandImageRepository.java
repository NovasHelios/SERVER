package com.heilous.land.repository;

import com.heilous.land.entity.LandImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandImageRepository extends JpaRepository<LandImage, Long> {
    List<LandImage> findByLandId(Long landId);
    List<LandImage> findByIdIn(List<Long> ids);
}
