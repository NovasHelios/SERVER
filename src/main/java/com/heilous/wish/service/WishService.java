package com.heilous.wish.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import com.heilous.user.entity.User;
import com.heilous.user.repository.UserRepository;
import com.heilous.wish.dto.WishResponse;
import com.heilous.wish.entity.Wish;
import com.heilous.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepository;
    private final LandRepository landRepository;
    private final UserRepository userRepository;

    // 찜 추가
    @Transactional
    public void addWish(Long landId, String email) {
        User user = getUser(email);

        if (wishRepository.existsByUserIdAndLandId(user.getId(), landId)) {
            throw new CustomException(GlobalErrorCode.WISH_ALREADY_EXISTS);
        }

        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        wishRepository.save(Wish.builder().user(user).land(land).build());
    }

    // 찜 취소
    @Transactional
    public void removeWish(Long landId, String email) {
        User user = getUser(email);

        Wish wish = wishRepository.findByUserIdAndLandId(user.getId(), landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.WISH_NOT_FOUND));

        wishRepository.delete(wish);
    }

    // 내 찜 목록 조회
    @Transactional(readOnly = true)
    public List<WishResponse> getMyWishes(String email) {
        User user = getUser(email);
        return wishRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream().map(WishResponse::from).toList();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }
}
