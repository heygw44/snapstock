package com.snapstock.domain.product.dto;

import com.snapstock.domain.product.entity.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String name,
        String description,
        int originalPrice,
        int stock,
        String category,
        LocalDateTime createdAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getCategory(),
                product.getCreatedAt()
        );
    }
}
