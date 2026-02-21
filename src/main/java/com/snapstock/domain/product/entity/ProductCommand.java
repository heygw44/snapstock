package com.snapstock.domain.product.entity;

public record ProductCommand(
        String name,
        String description,
        Integer originalPrice,
        Integer stock,
        String category
) {
}
