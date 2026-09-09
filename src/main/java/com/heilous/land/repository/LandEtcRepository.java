package com.heilous.land.repository;

import com.heilous.land.entity.LandEtc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandEtcRepository extends JpaRepository<LandEtc, Long> {
    void deleteByLandId(Long landId);
}
