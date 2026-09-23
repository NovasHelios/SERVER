package com.heilous.wish.repository;

import com.heilous.wish.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByUserIdAndLandId(Long userId, Long landId);

    Optional<Wish> findByUserIdAndLandId(Long userId, Long landId);

    @Query("select w from Wish w join fetch w.land where w.user.id = :userId order by w.id desc")
    List<Wish> findByUserIdOrderByIdDesc(@Param("userId") Long userId);
}
