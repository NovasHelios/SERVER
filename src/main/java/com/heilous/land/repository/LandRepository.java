package com.heilous.land.repository;

import com.heilous.land.entity.Land;
import com.heilous.land.entity.Land.LandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LandRepository extends JpaRepository<Land, Long>, JpaSpecificationExecutor<Land> {

    @Query("select l from Land l join fetch l.owner order by l.id desc")
    List<Land> findAllByOrderByIdDesc();

    @Query("select l from Land l join fetch l.owner where l.status = :status order by l.id desc")
    List<Land> findByStatusOrderByIdDesc(@Param("status") LandStatus status);

    boolean existsByAddress(String address);

    @Query("select l from Land l join fetch l.owner where l.owner.email = :email order by l.id desc")
    List<Land> findByOwnerEmailOrderByIdDesc(@Param("email") String email);

    @Query("select l from Land l join fetch l.owner left join fetch l.landImages where l.id = :id")
    Optional<Land> findWithImagesById(@Param("id") Long id);

    @Query("select l from Land l left join fetch l.landZones where l.id = :id")
    Optional<Land> findWithZonesById(@Param("id") Long id);

    @Query("select l from Land l left join fetch l.landEtcs where l.id = :id")
    Optional<Land> findWithEtcsById(@Param("id") Long id);

    /** 시/도, 시/군/구, 거래유형별 토지 수 집계 */
    @Query("select l.regionSido, l.regionSigungu, l.transactionType, count(l) " +
           "from Land l " +
           "where l.regionSido is not null and l.regionSigungu is not null " +
           "group by l.regionSido, l.regionSigungu, l.transactionType")
    List<Object[]> countByRegionAndTransactionType();
}
