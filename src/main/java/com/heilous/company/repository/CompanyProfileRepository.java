package com.heilous.company.repository;

import com.heilous.company.entity.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {
    boolean existsByBusinessNumber(String businessNumber);
}