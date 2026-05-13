package com.heilous.apply.repository;

import com.heilous.apply.entity.LandApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandApplyRepository
        extends JpaRepository<LandApply, Long> {

    List<LandApply> findByLandIdOrderByIdDesc(Long landId);

    List<LandApply> findByCompanyEmailOrderByIdDesc(String email);
}