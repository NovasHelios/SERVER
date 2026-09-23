package com.heilous;

import com.heilous.user.dto.UpdateProfileRequest;
import com.heilous.user.entity.User;
import com.heilous.user.enums.UserRole;
import com.heilous.user.repository.UserRepository;
import com.heilous.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataIntegrityIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private EntityManager entityManager;

    @Test
    void duplicateEmailIsRejectedByDatabaseConstraint() {
        saveUser("duplicate@example.com", "첫 사용자", "010-1111-1111");

        assertThatThrownBy(() -> {
            saveUser("duplicate@example.com", "두 번째 사용자", "010-2222-2222");
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void profileUpdateIsPersisted() {
        User user = saveUser("update@example.com", "수정 전", "010-1111-1111");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("수정 후");
        request.setPhone("010-9999-9999");

        userService.updateProfile(user.getEmail(), request);
        userRepository.flush();
        entityManager.clear();

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("수정 후");
        assertThat(updated.getPhone()).isEqualTo("010-9999-9999");
    }

    @Test
    void accountDeletionKeepsRecordAndDeactivatesIt() {
        User user = saveUser("delete@example.com", "삭제 대상", "010-3333-3333");

        userService.deleteUser(user.getId(), user.getEmail());
        userRepository.flush();
        entityManager.clear();

        User deactivated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    private User saveUser(String email, String name, String phone) {
        return userRepository.save(User.builder()
                .email(email).password("encoded-password").name(name).phone(phone)
                .role(UserRole.USER).isVerified(true).isActive(true).build());
    }
}
