package com.heilous.land.repository;

import com.heilous.land.entity.Land;
import com.heilous.land.entity.Land.LandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandRepository extends JpaRepository<Land, Long> {

    List<Land> findAllByOrderByIdDesc();

    List<Land> findByStatusOrderByIdDesc(LandStatus status);
}