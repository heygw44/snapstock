package com.snapstock.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = ProductCreateRequest.NAME_MAX_LENGTH, message = "상품명은 {max}자 이하여야 합니다.")
        String name,

        String description,

        @NotNull(message = "가격은 필수입니다.")
        @Positive(message = "가격은 0보다 커야 합니다.")
        Integer originalPrice,

        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        int stock,

        @NotBlank(message = "카테고리는 필수입니다.")
        @Size(max = ProductCreateRequest.CATEGORY_MAX_LENGTH, message = "카테고리는 {max}자 이하여야 합니다.")
        String category
) {

    private static final int NAME_MAX_LENGTH = 255;
    private static final int CATEGORY_MAX_LENGTH = 100;
}
