package com.heilous.land.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LandService {

    private final LandRepository landRepository;
    private final UserRepository userRepository;

    // 토지 등록
    @Transactional
    public void registerLand(LandRegisterRequest request, String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

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

    // 전체 조회
    @Transactional(readOnly = true)
    public List<LandResponse> getAllLands() {

        return landRepository.findAllByOrderByIdDesc()
                .stream()
                .map(LandResponse::from)
                .toList();
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public LandResponse getLand(Long landId) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        return LandResponse.from(land);
    }

    // 상태별 조회
    @Transactional(readOnly = true)
    public List<LandResponse> getLandsByStatus(String status) {

        Land.LandStatus landStatus;
        try {
            landStatus = Land.LandStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        return landRepository
                .findByStatusOrderByIdDesc(landStatus)
                .stream()
                .map(LandResponse::from)
                .toList();
    }

    // 토지 수정
    @Transactional
    public void updateLand(
            Long landId,
            LandUpdateRequest request,
            String email
    ) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (!land.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        land.updateLand(
                request.getAddress(),
                request.getArea(),
                request.getDesiredPrice(),
                request.getDescription()
        );
    }

    // 토지 삭제
    @Transactional
    public void deleteLand(
            Long landId,
            String email
    ) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (!land.getOwner().getEmail().equals(email)
                && user.getRole() != UserRole.ADMIN) {

            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        landRepository.delete(land);
    }

    // 관리자 승인
    @Transactional
    public void approveLand(
            Long landId,
            String email
    ) {

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        land.changeStatus(Land.LandStatus.APPROVED);
    }

    // 관리자 거절
    @Transactional
    public void rejectLand(
            Long landId,
            String email
    ) {

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        land.changeStatus(Land.LandStatus.REJECTED);
    }
}