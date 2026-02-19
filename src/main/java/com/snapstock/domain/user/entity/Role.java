package com.snapstock.domain.user.entity;

public enum Role {

    USER,
    ADMIN;

    private static final String ROLE_PREFIX = "ROLE_";

    public String getAuthority() {
        return ROLE_PREFIX + name();
    }
}
