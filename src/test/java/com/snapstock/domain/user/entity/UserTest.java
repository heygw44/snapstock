package com.snapstock.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("User.create로 생성하면 기본 role은 USER이다")
    void create_기본_role_USER() {
        // given

        // when
        User user = User.create("test@email.com", "encodedPassword", "tester");

        // then
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("User.create로 생성하면 email, password, nickname이 설정된다")
    void create_필드_설정() {
        // given

        // when
        User user = User.create("test@email.com", "encodedPassword", "tester");

        // then
        assertThat(user.getEmail()).isEqualTo("test@email.com");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("User.create로 생성 직후 deletedAt은 null이다")
    void create_deletedAt_null() {
        // given

        // when
        User user = User.create("test@email.com", "encodedPassword", "tester");

        // then
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("softDelete 호출하면 deletedAt이 설정된다")
    void softDelete_deletedAt_설정() {
        // given
        User user = User.create("test@email.com", "encodedPassword", "tester");

        // when
        user.softDelete();

        // then
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isDeleted()).isTrue();
    }
}
