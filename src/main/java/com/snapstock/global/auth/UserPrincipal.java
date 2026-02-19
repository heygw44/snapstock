package com.snapstock.global.auth;

import com.snapstock.domain.user.entity.Role;

public record UserPrincipal(Long userId, Role role) {
}
