package com.snapstock.global.auth;

import com.snapstock.domain.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("UserPrincipal은 userId와 role을 보관한다")
    void userPrincipal_생성_필드확인() {
        // given
        Long userId = 1L;
        Role role = Role.USER;

        // when
        UserPrincipal principal = new UserPrincipal(userId, role);

        // then
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo(role);
    }
}
