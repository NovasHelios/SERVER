package com.heilous.land.repository;

import com.heilous.land.entity.LandZone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandZoneRepository extends JpaRepository<LandZone, Long> {
    void deleteByLandId(Long landId);
}
