package com.snapstock.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = NICKNAME_MIN_LENGTH, max = NICKNAME_MAX_LENGTH,
                message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH,
                message = "비밀번호는 8~20자여야 합니다.")
        @Pattern(regexp = PASSWORD_PATTERN,
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.")
        String password
) {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 20;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 20;
    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$";

    public boolean hasUpdatableField() {
        return nickname != null || password != null;
    }
}
