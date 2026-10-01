package com.heilous.business.repository;

import com.heilous.business.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    Optional<BusinessProfile> findByUserEmail(String email);
    boolean existsByUserEmail(String email);
    boolean existsByBusinessNumber(String businessNumber);
}
