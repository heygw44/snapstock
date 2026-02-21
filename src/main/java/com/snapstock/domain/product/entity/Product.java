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

    private static final int CATEGORY_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
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

    private Product(String name, String description, int originalPrice,
                    int stock, String category) {
        this.name = name;
        this.description = description;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.category = category;
    }

    public static Product create(String name, String description, int originalPrice,
                                  int stock, String category) {
        return new Product(name, description, originalPrice, stock, category);
    }

    public void update(String name, String description, int originalPrice,
                       int stock, String category) {
        this.name = name;
        this.description = description;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.category = category;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
