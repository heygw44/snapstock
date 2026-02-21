package com.snapstock.domain.product.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        @Test
        void 가격이_0이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create("상품A", "설명", 0, 50, "전자제품"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가격은 0보다 커야 합니다.");
        }

        @Test
        void 음수_가격이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create("상품A", "설명", -1, 50, "전자제품"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가격은 0보다 커야 합니다.");
        }

        @Test
        void 음수_재고이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create("상품A", "설명", 10000, -1, "전자제품"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고는 0 이상이어야 합니다.");
        }

        @Test
        void 상품명이_null이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create(null, "설명", 10000, 50, "전자제품"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품명은 필수입니다.");
        }

        @Test
        void 상품명이_빈값이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create("  ", "설명", 10000, 50, "전자제품"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품명은 필수입니다.");
        }

        @Test
        void 카테고리가_null이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create("상품A", "설명", 10000, 50, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리는 필수입니다.");
        }

        @Test
        void 카테고리가_빈값이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> Product.create("상품A", "설명", 10000, 50, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리는 필수입니다.");
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

        @Test
        void 가격0으로_수정하면_예외가_발생한다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when & then
            assertThatThrownBy(() -> product.update("상품B", "새 설명", 0, 100, "의류"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가격은 0보다 커야 합니다.");
        }

        @Test
        void 음수_가격으로_수정하면_예외가_발생한다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when & then
            assertThatThrownBy(() -> product.update("상품B", "새 설명", -1, 100, "의류"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가격은 0보다 커야 합니다.");
        }

        @Test
        void 음수_재고로_수정하면_예외가_발생한다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when & then
            assertThatThrownBy(() -> product.update("상품B", "새 설명", 20000, -1, "의류"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고는 0 이상이어야 합니다.");
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
        void 이미_삭제된_상품은_deletedAt이_변경되지_않는다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");
            product.softDelete();
            LocalDateTime firstDeletedAt = product.getDeletedAt();

            // when
            product.softDelete();

            // then
            assertThat(product.getDeletedAt()).isEqualTo(firstDeletedAt);
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
