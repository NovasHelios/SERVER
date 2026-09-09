package com.heilous.land.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.common.service.ImageStorageService;
import com.heilous.land.dto.LandDetailResponse;
import com.heilous.land.dto.LandFilterRequest;
import com.heilous.land.dto.LandRegisterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.LandUpdateRequest;
import com.heilous.land.entity.Land;
import com.heilous.land.entity.LandEtc;
import com.heilous.land.entity.LandImage;
import com.heilous.land.entity.LandZone;
import com.heilous.land.repository.LandImageRepository;
import com.heilous.land.repository.LandRepository;
import com.heilous.land.repository.LandSpecification;
import com.heilous.land.repository.LandZoneRepository;
import com.heilous.land.repository.LandEtcRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import com.heilous.vworld.dto.AddressLandResponse;
import com.heilous.vworld.dto.VWorldLandResponse;
import com.heilous.vworld.dto.VWorldWfsResponse;
import com.heilous.vworld.service.VWorldService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LandService {

    private final LandRepository landRepository;
    private final LandImageRepository landImageRepository;
    private final LandZoneRepository landZoneRepository;
    private final LandEtcRepository landEtcRepository;
    private final UserRepository userRepository;
    private final VWorldService vWorldService;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    // 토지 등록
    @Transactional
    public void registerLand(LandRegisterRequest request, String email, MultipartFile document, List<MultipartFile> images) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (owner.getRole() != UserRole.USER) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        if (landRepository.existsByAddress(request.getAddress())) {
            throw new CustomException(GlobalErrorCode.LAND_ADDRESS_ALREADY_EXISTS);
        }

        // 이미지 개수 검증 (최소 3장, 최대 5장)
        List<MultipartFile> validImages = images == null ? List.of() :
                images.stream().filter(f -> f != null && !f.isEmpty()).toList();

        if (validImages.size() < 3) {
            throw new CustomException(GlobalErrorCode.LAND_IMAGE_REQUIRED);
        }
        if (validImages.size() > 5) {
            throw new CustomException(GlobalErrorCode.LAND_IMAGE_MAX_EXCEEDED);
        }

        // VWorld API로 면적, 지목코드, 지목명 자동 조회
        AddressLandResponse addressLandResponse = vWorldService.getLandInfoByAddress(request.getAddress());
        VWorldLandResponse landInfo = addressLandResponse.getLandInfo();

        Double area = null;
        String lcCode = null;
        String lcCodeNm = null;
        String lastUpdtDt = null;
        String regstrSeCodeNm = null;
        String cnrsPsnCo = null;
        String pnu = null;
        String ldCodeNm = null;

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
            lastUpdtDt = info.getLastUpdtDt();
            regstrSeCodeNm = info.getRegstrSeCodeNm();
            cnrsPsnCo = info.getCnrsPsnCo();
            pnu = info.getPnu();
            ldCodeNm = info.getLdCodeNm();
        }

        String[] regions = parseRegions(ldCodeNm);

        Land land = Land.builder()
                .owner(owner)
                .address(addressLandResponse.getAddressName())
                .area(area)
                .lcCode(lcCode)
                .lcCodeNm(lcCodeNm)
                .lastUpdtDt(lastUpdtDt)
                .regstrSeCodeNm(regstrSeCodeNm)
                .cnrsPsnCo(cnrsPsnCo)
                .pnu(pnu)
                .ldCodeNm(ldCodeNm)
                .regionSido(regions[0])
                .regionSigungu(regions[1])
                .regionEupmyeondong(regions[2])
                .desiredPrice(request.getTransactionType() == Land.TransactionType.BUSINESS
                        ? null : request.getDesiredPrice())
                .description(request.getDescription())
                .transactionType(request.getTransactionType())
                .status(Land.LandStatus.PENDING)
                .x(addressLandResponse.getX())
                .y(addressLandResponse.getY())
                .build();

        landRepository.save(land);

        // WFS: 용도지역/지구/기타 조회 및 저장
        if (pnu != null) {
            applyLandUseInfo(land, pnu);
        }

        // 이미지 저장
        for (MultipartFile image : validImages) {
            String filename = imageStorageService.store(image, "lands");
            LandImage landImage = LandImage.builder()
                    .land(land)
                    .imagePath(filename)
                    .build();
            landImageRepository.save(landImage);
        }

        if (document != null && !document.isEmpty()) {
            String documentPath = imageStorageService.storeDocument(document, "documents");
            land.updateDocumentPath(documentPath);
        }
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
    public LandDetailResponse getLand(Long landId) {

        // MultipleBagFetchException 방지: 컬렉션별 쿼리 분리
        Land land = landRepository.findWithImagesById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        // zones, etcs는 별도 쿼리로 로딩 (hibernate 1차 캐시로 같은 엔티티에 merge됨)
        landRepository.findWithZonesById(landId);
        landRepository.findWithEtcsById(landId);

        return LandDetailResponse.from(land);
    }

    // 필터 조회
    @Transactional(readOnly = true)
    public List<LandResponse> getLandsByFilter(LandFilterRequest filter) {

        return landRepository
                .findAll(LandSpecification.buildFilter(filter))
                .stream()
                .map(LandResponse::from)
                .toList();
    }

    // 내 토지 조회
    @Transactional(readOnly = true)
    public List<LandResponse> getMyLands(String email) {
        return landRepository.findByOwnerEmailOrderByIdDesc(email)
                .stream()
                .map(LandResponse::from)
                .toList();
    }

    // ldCodeNm → [시/도, 시/군/구, 읍/면/동] 파싱
    private String[] parseRegions(String ldCodeNm) {
        String[] result = new String[3];
        if (ldCodeNm == null || ldCodeNm.isBlank()) return result;

        String[] parts = ldCodeNm.trim().split("\\s+");
        if (parts.length > 0) result[0] = parts[0];
        if (parts.length > 1) result[1] = parts[1];
        if (parts.length > 2) result[2] = parts[2];
        return result;
    }

    /**
     * WFS 결과를 파싱해 Land 엔티티에 용도지역/지구/기타 정보를 저장
     */
    private void applyLandUseInfo(Land land, String pnu) {
        try {
            VWorldWfsResponse wfs = vWorldService.getLandUseByPnu(pnu);
            if (wfs == null || wfs.getFeatures() == null || wfs.getFeatures().isEmpty()) return;

            VWorldWfsResponse.Feature feature = wfs.getFeatures().get(0);
            VWorldWfsResponse.Properties props = feature.getProperties();

            if (props == null || props.getPrposAreaDstrcCodeList() == null) return;

            String[] codes   = props.getPrposAreaDstrcCodeList().split(",");
            String[] cnflcs  = props.getCnflcAtList() != null
                    ? props.getCnflcAtList().split(",") : new String[0];
            String[] cnflcNms = props.getCnflcAtNmList() != null
                    ? props.getCnflcAtNmList().split(",") : new String[0];

            // 좌표 JSON 직렬화
            String coordinatesJson = null;
            if (feature.getGeometry() != null && feature.getGeometry().getCoordinates() != null) {
                coordinatesJson = objectMapper.writeValueAsString(feature.getGeometry().getCoordinates());
            }

            String prposAreaCode = null, prposAreaCnflcAt = null, prposAreaCnflcAtNm = null;

            List<LandZone> zones = new ArrayList<>();
            List<LandEtc> etcs  = new ArrayList<>();

            for (int i = 0; i < codes.length; i++) {
                String code     = codes[i].trim();
                String cnflc    = i < cnflcs.length  ? cnflcs[i].trim()   : null;
                String cnflcNm  = i < cnflcNms.length ? cnflcNms[i].trim() : null;

                if (isZoneCode(code)) {         // UQA~UQE: 용도지역
                    if (prposAreaCode == null) {
                        prposAreaCode = code;
                        prposAreaCnflcAt = cnflc;
                        prposAreaCnflcAtNm = cnflcNm;
                    }
                } else if (isDistrictCode(code)) { // UQF~UQP: 용도지구
                    zones.add(LandZone.builder()
                            .land(land)
                            .code(code)
                            .cnflcAt(cnflc)
                            .cnflcAtNm(cnflcNm)
                            .build());
                } else {                           // 나머지: 기타
                    etcs.add(LandEtc.builder()
                            .land(land)
                            .code(code)
                            .cnflcAt(cnflc)
                            .cnflcAtNm(cnflcNm)
                            .build());
                }
            }

            land.updateLandUse(prposAreaCode, prposAreaCnflcAt, prposAreaCnflcAtNm, coordinatesJson);

            if (!zones.isEmpty()) landZoneRepository.saveAll(zones);
            if (!etcs.isEmpty())  landEtcRepository.saveAll(etcs);

        } catch (Exception e) {
            log.warn("WFS 용도지역 정보 저장 실패 (pnu={}): {}", pnu, e.getMessage());
        }
    }

    /** UQA ~ UQE: 용도지역 */
    private boolean isZoneCode(String code) {
        if (code == null || code.length() < 3) return false;
        String prefix = code.substring(0, 3).toUpperCase();
        return prefix.compareTo("UQA") >= 0 && prefix.compareTo("UQF") < 0;
    }

    /** UQF ~ UQP: 용도지구 */
    private boolean isDistrictCode(String code) {
        if (code == null || code.length() < 3) return false;
        String prefix = code.substring(0, 3).toUpperCase();
        return prefix.compareTo("UQF") >= 0 && prefix.compareTo("UQQ") < 0;
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
        String lastUpdtDt = null;
        String regstrSeCodeNm = null;
        String cnrsPsnCo = null;
        String pnu = null;
        String ldCodeNm = null;

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
            lastUpdtDt = info.getLastUpdtDt();
            regstrSeCodeNm = info.getRegstrSeCodeNm();
            cnrsPsnCo = info.getCnrsPsnCo();
            pnu = info.getPnu();
            ldCodeNm = info.getLdCodeNm();
        }

        String[] regions = parseRegions(ldCodeNm);

        land.updateLand(
                addressLandResponse.getAddressName(),
                area,
                lcCode,
                lcCodeNm,
                lastUpdtDt,
                regstrSeCodeNm,
                cnrsPsnCo,
                pnu,
                ldCodeNm,
                regions[0],
                regions[1],
                regions[2],
                request.getDesiredPrice(),
                request.getDescription(),
                request.getTransactionType(),
                addressLandResponse.getX(),
                addressLandResponse.getY()
        );

        // WFS: 용도지역/지구/기타 재조회
        if (pnu != null) {
            landZoneRepository.deleteByLandId(land.getId());
            landEtcRepository.deleteByLandId(land.getId());
            applyLandUseInfo(land, pnu);
        }
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

        // 이미지 파일 삭제
        land.getLandImages().forEach(img ->
                imageStorageService.delete(img.getImagePath(), "lands"));

        // 증명서 파일 삭제
        if (land.getDocumentPath() != null) {
            imageStorageService.delete(land.getDocumentPath(), "documents");
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

    // 토지 이미지 추가/수정
    @Transactional
    public void uploadLandImage(Long landId, Long imageId, MultipartFile image, String email) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (!land.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        String filename = imageStorageService.store(image, "lands");

        if (imageId != null) {
            // 수정: 기존 이미지 교체
            LandImage existing = landImageRepository.findById(imageId)
                    .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_IMAGE_NOT_FOUND));

            imageStorageService.delete(existing.getImagePath(), "lands");
            land.removeImage(existing);
            landImageRepository.delete(existing);
        } else {
            // 추가: 최대 5장 초과 여부 확인
            if (land.getLandImages().size() >= 5) {
                throw new CustomException(GlobalErrorCode.LAND_IMAGE_MAX_EXCEEDED);
            }
        }

        LandImage newImage = LandImage.builder()
                .land(land)
                .imagePath(filename)
                .build();
        landImageRepository.save(newImage);
    }

    // 증명서 경로 조회 (어드민용)
    @Transactional(readOnly = true)
    public String getDocumentPath(Long landId, String email) {
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        Land land = landRepository.findById(landId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (land.getDocumentPath() == null) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        return land.getDocumentPath();
    }
}