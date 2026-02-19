package com.snapstock.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email @Size(max = EMAIL_MAX_LENGTH)
        String email,

        @NotBlank
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
        @Pattern(regexp = PASSWORD_PATTERN, message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.")
        String password,

        @NotBlank
        @Size(min = NICKNAME_MIN_LENGTH, max = NICKNAME_MAX_LENGTH)
        String nickname
) {

    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 20;
    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$";
    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 20;
}
