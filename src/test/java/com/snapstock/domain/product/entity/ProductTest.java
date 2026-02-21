package com.snapstock.domain.product.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Nested
    class create {

        @Test
        void 모든_필드가_설정된다() {
            // when
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // then
            assertThat(product.getName()).isEqualTo("상품A");
            assertThat(product.getDescription()).isEqualTo("설명");
            assertThat(product.getOriginalPrice()).isEqualTo(10000);
            assertThat(product.getStock()).isEqualTo(50);
            assertThat(product.getCategory()).isEqualTo("전자제품");
        }

        @Test
        void deletedAt은_null이다() {
            // when
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // then
            assertThat(product.getDeletedAt()).isNull();
        }
    }

    @Nested
    class update {

        @Test
        void 모든_필드가_변경된다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when
            product.update("상품B", "새 설명", 20000, 100, "의류");

            // then
            assertThat(product.getName()).isEqualTo("상품B");
            assertThat(product.getDescription()).isEqualTo("새 설명");
            assertThat(product.getOriginalPrice()).isEqualTo(20000);
            assertThat(product.getStock()).isEqualTo(100);
            assertThat(product.getCategory()).isEqualTo("의류");
        }
    }

    @Nested
    class softDelete {

        @Test
        void deletedAt이_설정된다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when
            product.softDelete();

            // then
            assertThat(product.getDeletedAt()).isNotNull();
        }

        @Test
        void isDeleted가_true를_반환한다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when
            product.softDelete();

            // then
            assertThat(product.isDeleted()).isTrue();
        }
    }

    @Nested
    class isDeleted {

        @Test
        void 생성_직후_false를_반환한다() {
            // when
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // then
            assertThat(product.isDeleted()).isFalse();
        }
    }
}
