package com.heilous;

import com.heilous.apply.dto.ApplyRequest;
import com.heilous.apply.repository.LandApplyRepository;
import com.heilous.apply.service.ApplyService;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.entity.Land;
import com.heilous.land.repository.LandRepository;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import com.heilous.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAuthorizationIntegrationTest {

    @Autowired private ApplyService applyService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private LandRepository landRepository;
    @Autowired private LandApplyRepository landApplyRepository;

    @Test
    void onlyCompanyCanApplyForLand() {
        User owner = saveUser("owner@example.com", UserRole.USER);
        User regularUser = saveUser("regular@example.com", UserRole.USER);
        Land land = landRepository.save(land(owner));

        assertThatThrownBy(() -> applyService.applyLand(land.getId(), new ApplyRequest(), regularUser.getEmail()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(GlobalErrorCode.ACCESS_DENIED);
        assertThat(landApplyRepository.count()).isZero();

        User company = saveUser("company@example.com", UserRole.COMPANY);
        applyService.applyLand(land.getId(), new ApplyRequest(), company.getEmail());

        assertThat(landApplyRepository.findByCompanyEmailOrderByIdDesc(company.getEmail())).hasSize(1);
    }

    @Test
    void regularUserCannotDeactivateAnotherAccountButAdminCan() {
        User target = saveUser("target@example.com", UserRole.USER);
        User regularUser = saveUser("regular@example.com", UserRole.USER);
        User admin = saveUser("admin@example.com", UserRole.ADMIN);

        assertThatThrownBy(() -> userService.deleteUser(target.getId(), regularUser.getEmail()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(GlobalErrorCode.ACCESS_DENIED);
        assertThat(userRepository.findById(target.getId())).get().extracting(User::isActive).isEqualTo(true);

        userService.deleteUser(target.getId(), admin.getEmail());

        assertThat(userRepository.findById(target.getId())).get().extracting(User::isActive).isEqualTo(false);
    }

    private User saveUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .email(email).password("encoded-password").name("테스트 사용자").phone("010-0000-0000")
                .role(role).isVerified(true).isActive(true).build());
    }

    private Land land(User owner) {
        return Land.builder().owner(owner).address("서울특별시 종로구 테스트로 1")
                .transactionType(Land.TransactionType.SALE).status(Land.LandStatus.APPROVED)
                .x(126.9780).y(37.5665).build();
    }
}
