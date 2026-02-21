package com.snapstock.domain.product.dto;

import com.snapstock.domain.product.entity.Product;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @Size(max = Product.NAME_MAX_LENGTH,
                message = "상품명은 {max}자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*",
                message = "상품명은 공백만으로 구성할 수 없습니다.")
        String name,

        String description,

        @Positive(message = "가격은 0보다 커야 합니다.")
        Integer originalPrice,

        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer stock,

        @Size(max = Product.CATEGORY_MAX_LENGTH,
                message = "카테고리는 {max}자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*",
                message = "카테고리는 공백만으로 구성할 수 없습니다.")
        String category
) {

    public boolean hasUpdatableField() {
        return name != null || description != null
                || originalPrice != null || stock != null || category != null;
    }
}
