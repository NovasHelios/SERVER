package com.heilous.land.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.common.service.ImageStorageService;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import com.heilous.vworld.dto.AddressLandResponse;
import com.heilous.vworld.dto.VWorldLandResponse;
import com.heilous.vworld.service.VWorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LandService {

    private final LandRepository landRepository;
    private final UserRepository userRepository;
    private final VWorldService vWorldService;
    private final ImageStorageService imageStorageService;

    // 토지 등록
    @Transactional
    public void registerLand(LandRegisterRequest request, String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (owner.getRole() != UserRole.USER) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        // VWorld API로 면적, 지목코드, 지목명 자동 조회
        AddressLandResponse addressLandResponse = vWorldService.getLandInfoByAddress(request.getAddress());
        VWorldLandResponse landInfo = addressLandResponse.getLandInfo();

        Double area = null;
        String lcCode = null;
        String lcCodeNm = null;

        if (landInfo != null
                && landInfo.getLadfrlVOList() != null
                && landInfo.getLadfrlVOList().getLadfrlVOList() != null
                && !landInfo.getLadfrlVOList().getLadfrlVOList().isEmpty()) {

            VWorldLandResponse.LandInfo info = landInfo.getLadfrlVOList().getLadfrlVOList().get(0);

            try {
                area = Double.parseDouble(info.getLndpclAr());
            } catch (NumberFormatException | NullPointerException ignored) {}

            lcCode = info.getLndcgrCode();
            lcCodeNm = info.getLndcgrCodeNm();
        }

        Land land = Land.builder()
                .owner(owner)
                .address(request.getAddress())
                .area(area)
                .lcCode(lcCode)
                .lcCodeNm(lcCodeNm)
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

        // 주소가 변경된 경우 VWorld에서 면적, 지목 재조회
        AddressLandResponse addressLandResponse = vWorldService.getLandInfoByAddress(request.getAddress());
        VWorldLandResponse landInfo = addressLandResponse.getLandInfo();

        Double area = null;
        String lcCode = null;
        String lcCodeNm = null;

        if (landInfo != null
                && landInfo.getLadfrlVOList() != null
                && landInfo.getLadfrlVOList().getLadfrlVOList() != null
                && !landInfo.getLadfrlVOList().getLadfrlVOList().isEmpty()) {

            VWorldLandResponse.LandInfo info = landInfo.getLadfrlVOList().getLadfrlVOList().get(0);

            try {
                area = Double.parseDouble(info.getLndpclAr());
            } catch (NumberFormatException | NullPointerException ignored) {}

            lcCode = info.getLndcgrCode();
            lcCodeNm = info.getLndcgrCodeNm();
        }

        land.updateLand(
                request.getAddress(),
                area,
                lcCode,
                lcCodeNm,
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

    // 토지 이미지 업로드
    @Transactional
    public void uploadLandImage(Long landId, MultipartFile image, String email) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (!land.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        // 기존 이미지 삭제
        imageStorageService.delete(land.getLandImagePath(), "lands");

        String filename = imageStorageService.store(image, "lands");
        land.updateImagePath(filename);
    }
}