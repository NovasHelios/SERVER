package com.heilous.land.repository;

import com.heilous.land.entity.LandPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandPriceRepository extends JpaRepository<LandPrice, Long> {
    void deleteByLandId(Long landId);
}
