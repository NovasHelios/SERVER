package com.heilous.wish.repository;

import com.heilous.wish.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByUserIdAndLandId(Long userId, Long landId);

    Optional<Wish> findByUserIdAndLandId(Long userId, Long landId);

    List<Wish> findByUserIdOrderByIdDesc(Long userId);
}
