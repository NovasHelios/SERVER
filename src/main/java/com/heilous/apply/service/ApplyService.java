package com.heilous.apply.service;

import com.heilous.apply.dto.ApplyRequest;
import com.heilous.apply.dto.ApplyResponse;
import com.heilous.apply.entity.ApplyStatus;
import com.heilous.apply.entity.LandApply;
import com.heilous.apply.repository.LandApplyRepository;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
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
public class ApplyService {

    private final LandApplyRepository applyRepository;
    private final LandRepository landRepository;
    private final UserRepository userRepository;

    // 신청
    @Transactional
    public void applyLand(
            Long landId,
            ApplyRequest request,
            String email
    ) {

        User company = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (company.getRole() != UserRole.COMPANY) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        LandApply apply = LandApply.builder()
                .company(company)
                .land(land)
                .message(request.getMessage())
                .status(ApplyStatus.PENDING)
                .build();

        applyRepository.save(apply);
    }

    // 토지 신청 목록 조회
    @Transactional(readOnly = true)
    public List<ApplyResponse> getApplies(
            Long landId,
            String email
    ) {

        Land land = landRepository.findById(landId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        if (!land.getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        return applyRepository.findByLandIdOrderByIdDesc(landId)
                .stream()
                .map(ApplyResponse::from)
                .toList();
    }

    // 업체 신청 내역 조회
    @Transactional(readOnly = true)
    public List<ApplyResponse> myApplies(
            String email
    ) {

        return applyRepository.findByCompanyEmailOrderByIdDesc(email)
                .stream()
                .map(ApplyResponse::from)
                .toList();
    }

    // 승인
    @Transactional
    public void approveApply(
            Long applyId,
            String email
    ) {

        LandApply apply = applyRepository.findById(applyId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.INVALID_INPUT));

        if (!apply.getLand().getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        apply.changeStatus(ApplyStatus.APPROVED);
    }

    // 거절
    @Transactional
    public void rejectApply(
            Long applyId,
            String email
    ) {

        LandApply apply = applyRepository.findById(applyId)
                .orElseThrow(() ->
                        new CustomException(GlobalErrorCode.INVALID_INPUT));

        if (!apply.getLand().getOwner().getEmail().equals(email)) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        apply.changeStatus(ApplyStatus.REJECTED);
    }
}