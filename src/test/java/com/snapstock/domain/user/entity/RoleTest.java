package com.snapstock.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    @DisplayName("Role은 USER와 ADMIN 2개 값을 갖는다")
    void Role_값_2개_존재() {
        // when
        Role[] roles = Role.values();

        // then
        assertThat(roles).hasSize(2);
    }

    @Test
    @DisplayName("USER의 authority는 ROLE_USER이다")
    void USER_authority_ROLE_USER() {
        // when
        String authority = Role.USER.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("ADMIN의 authority는 ROLE_ADMIN이다")
    void ADMIN_authority_ROLE_ADMIN() {
        // when
        String authority = Role.ADMIN.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_ADMIN");
    }
}
