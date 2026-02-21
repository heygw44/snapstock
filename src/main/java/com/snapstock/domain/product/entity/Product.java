package com.snapstock.domain.product.entity;

import com.snapstock.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category_id", columnList = "category, id DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    private static final int NAME_MAX_LENGTH = 255;
    private static final int CATEGORY_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int originalPrice;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = CATEGORY_MAX_LENGTH)
    private String category;

    private LocalDateTime deletedAt;

    private Product(String name, String description, Integer originalPrice,
                    Integer stock, String category) {
        validateName(name);
        validateCategory(category);
        validatePrice(originalPrice);
        validateStock(stock);
        this.name = name;
        this.description = description;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.category = category;
    }

    public static Product create(String name, String description, Integer originalPrice,
                                  Integer stock, String category) {
        return new Product(name, description, originalPrice, stock, category);
    }

    public void update(String name, String description, Integer originalPrice,
                       Integer stock, String category) {
        validateName(name);
        validateCategory(category);
        validatePrice(originalPrice);
        validateStock(stock);
        this.name = name;
        this.description = description;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.category = category;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            return;
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "상품명은 " + NAME_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    private static void validateCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        if (category.length() > CATEGORY_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "카테고리는 " + CATEGORY_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    private static void validatePrice(Integer price) {
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
        }
    }

    private static void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
    }
}
