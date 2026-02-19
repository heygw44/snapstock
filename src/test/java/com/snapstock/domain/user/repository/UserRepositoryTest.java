package com.snapstock.domain.user.repository;

import com.snapstock.domain.user.entity.User;
import com.snapstock.support.JpaRepositorySliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositorySliceTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save 시 createdAt, updatedAt이 자동 설정된다")
    void save_audit_필드_자동설정() {
        // given
        User user = User.create("audit@email.com", "encodedPassword", "auditor");

        // when
        User saved = userRepository.save(user);

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail로 저장된 유저를 조회한다")
    void findByEmail_저장된_유저_조회() {
        // given
        User user = User.create("test@email.com", "encodedPassword", "tester");
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByEmail("test@email.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@email.com");
    }

    @Test
    @DisplayName("findByEmail로 존재하지 않는 이메일 조회 시 빈 Optional을 반환한다")
    void findByEmail_존재하지않는_이메일_빈Optional() {
        // when
        Optional<User> found = userRepository.findByEmail("nonexistent@email.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail은 존재하는 이메일이면 true를 반환한다")
    void existsByEmail_존재하면_true() {
        // given
        User user = User.create("test@email.com", "encodedPassword", "tester");
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByEmail("test@email.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail은 존재하지 않는 이메일이면 false를 반환한다")
    void existsByEmail_존재하지않으면_false() {
        // when
        boolean exists = userRepository.existsByEmail("nonexistent@email.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByNickname은 존재하는 닉네임이면 true를 반환한다")
    void existsByNickname_존재하면_true() {
        // given
        User user = User.create("test@email.com", "encodedPassword", "tester");
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByNickname("tester");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNickname은 존재하지 않는 닉네임이면 false를 반환한다")
    void existsByNickname_존재하지않으면_false() {
        // when
        boolean exists = userRepository.existsByNickname("nonexistent");

        // then
        assertThat(exists).isFalse();
    }
}
