package com.heilous.land.repository;

import com.heilous.land.entity.Land;
import com.heilous.land.entity.Land.LandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LandRepository extends JpaRepository<Land, Long> {

    @Query("select l from Land l join fetch l.owner order by l.id desc")
    List<Land> findAllByOrderByIdDesc();

    @Query("select l from Land l join fetch l.owner where l.status = :status order by l.id desc")
    List<Land> findByStatusOrderByIdDesc(@Param("status") LandStatus status);

    boolean existsByAddress(String address);
}
