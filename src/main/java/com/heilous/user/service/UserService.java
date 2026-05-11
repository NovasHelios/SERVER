package com.heilous.user.service;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 계정 삭제 (Soft Delete)
     * @param id 삭제할 유저의 PK
     * @param loginEmail 현재 로그인한 유저의 이메일 (SecurityContext에서 가져온 값)
     */
    @Transactional
    public void deleteUser(Long id, String loginEmail) {
        // 1. 삭제할 유저 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 2. 현재 로그인한 유저 조회 (권한 확인용)
        User currentUser = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 3. 본인이거나 관리자(ADMIN)인 경우만 삭제 가능하도록 예외 처리
        if (!user.getEmail().equals(loginEmail) && currentUser.getRole() != UserRole.ADMIN) {
            throw new CustomException(GlobalErrorCode.ACCESS_DENIED);
        }

        // 4. 실제 삭제 대신 비활성화 처리 (Soft Delete)
        user.deactivate();
    }

    // 여기에 프로필 수정 등의 로직을 추가하면 됩니다.
}