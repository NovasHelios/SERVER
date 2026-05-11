package com.heilous.land.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LandService {
    private final LandRepository landRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerLand(LandRegisterRequest request, String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 토지소유자(USER) 권한인지 확인
        if (owner.getRole() != UserRole.USER) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        Land land = Land.builder()
                .owner(owner)
                .address(request.getAddress())
                .area(request.getArea())
                .desiredPrice(request.getDesiredPrice())
                .description(request.getDescription())
                .status(Land.LandStatus.PENDING)
                .build();

        landRepository.save(land);
    }
}
