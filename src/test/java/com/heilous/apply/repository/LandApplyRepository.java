package com.heilous.apply.repository;

import com.heilous.apply.entity.LandApply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LandApplyRepository
        extends JpaRepository<LandApply, Long> {

    @Query("select a from LandApply a join fetch a.company where a.land.id = :landId order by a.id desc")
    List<LandApply> findByLandIdOrderByIdDesc(@Param("landId") Long landId);

    @Query("select a from LandApply a join fetch a.land join fetch a.company where a.company.email = :email order by a.id desc")
    List<LandApply> findByCompanyEmailOrderByIdDesc(@Param("email") String email);
}
