package com.snapstock.domain.product.repository;

import com.snapstock.domain.product.entity.Product;
import com.snapstock.support.JpaRepositorySliceTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositorySliceTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private static final Pageable PAGE_SIZE_3 = PageRequest.of(0, 3);

    @Nested
    class save {

        @Test
        void audit_필드가_자동_설정된다() {
            // given
            Product product = Product.create("상품A", "설명", 10000, 50, "전자제품");

            // when
            Product saved = productRepository.save(product);

            // then
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class findByIdAndDeletedAtIsNull {

        @Test
        void 존재하는_상품을_조회한다() {
            // given
            Product product = productRepository.save(
                    Product.create("상품A", "설명", 10000, 50, "전자제품"));

            // when
            Optional<Product> found = productRepository.findByIdAndDeletedAtIsNull(product.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("상품A");
        }

        @Test
        void softDelete된_상품은_조회되지_않는다() {
            // given
            Product product = productRepository.save(
                    Product.create("상품A", "설명", 10000, 50, "전자제품"));
            product.softDelete();
            productRepository.saveAndFlush(product);

            // when
            Optional<Product> found = productRepository.findByIdAndDeletedAtIsNull(product.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        void 존재하지_않는_id는_빈_Optional을_반환한다() {
            // when
            Optional<Product> found = productRepository.findByIdAndDeletedAtIsNull(999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    class 카테고리_필터_커서_페이지네이션 {

        @Test
        void 카테고리와_커서로_조회한다() {
            // given
            Product p1 = productRepository.save(Product.create("상품1", null, 1000, 10, "전자제품"));
            productRepository.save(Product.create("상품2", null, 2000, 20, "의류"));
            Product p3 = productRepository.save(Product.create("상품3", null, 3000, 30, "전자제품"));
            productRepository.save(Product.create("상품4", null, 4000, 40, "전자제품"));

            // when
            List<Product> result = productRepository
                    .findByCategoryAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(
                            "전자제품", p3.getId(), PAGE_SIZE_3);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(p1.getId());
        }

        @Test
        void 카테고리_첫_페이지를_조회한다() {
            // given
            productRepository.save(Product.create("상품1", null, 1000, 10, "전자제품"));
            productRepository.save(Product.create("상품2", null, 2000, 20, "의류"));
            productRepository.save(Product.create("상품3", null, 3000, 30, "전자제품"));

            // when
            List<Product> result = productRepository
                    .findByCategoryAndDeletedAtIsNullOrderByIdDesc("전자제품", PAGE_SIZE_3);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class 전체_커서_페이지네이션 {

        @Test
        void 커서_이후_상품을_조회한다() {
            // given
            Product p1 = productRepository.save(Product.create("상품1", null, 1000, 10, "전자제품"));
            Product p2 = productRepository.save(Product.create("상품2", null, 2000, 20, "의류"));
            Product p3 = productRepository.save(Product.create("상품3", null, 3000, 30, "전자제품"));

            // when
            List<Product> result = productRepository
                    .findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(p3.getId(), PAGE_SIZE_3);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(p2.getId());
            assertThat(result.get(1).getId()).isEqualTo(p1.getId());
        }

        @Test
        void 첫_페이지를_조회한다() {
            // given
            productRepository.save(Product.create("상품1", null, 1000, 10, "전자제품"));
            productRepository.save(Product.create("상품2", null, 2000, 20, "의류"));
            productRepository.save(Product.create("상품3", null, 3000, 30, "전자제품"));
            productRepository.save(Product.create("상품4", null, 4000, 40, "의류"));

            // when
            List<Product> result = productRepository
                    .findByDeletedAtIsNullOrderByIdDesc(PAGE_SIZE_3);

            // then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    class softDelete_필터링 {

        @Test
        void softDelete된_상품은_목록에서_제외된다() {
            // given
            Product p1 = productRepository.save(Product.create("상품1", null, 1000, 10, "전자제품"));
            Product p2 = productRepository.save(Product.create("상품2", null, 2000, 20, "전자제품"));
            p1.softDelete();
            productRepository.saveAndFlush(p1);

            // when
            List<Product> result = productRepository
                    .findByCategoryAndDeletedAtIsNullOrderByIdDesc("전자제품", PAGE_SIZE_3);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(p2.getId());
        }
    }

    @Nested
    class 정렬 {

        @Test
        void id_내림차순으로_정렬된다() {
            // given
            Product p1 = productRepository.save(Product.create("상품1", null, 1000, 10, "전자제품"));
            Product p2 = productRepository.save(Product.create("상품2", null, 2000, 20, "전자제품"));
            Product p3 = productRepository.save(Product.create("상품3", null, 3000, 30, "전자제품"));

            // when
            List<Product> result = productRepository
                    .findByDeletedAtIsNullOrderByIdDesc(PAGE_SIZE_3);

            // then
            assertThat(result).extracting(Product::getId)
                    .containsExactly(p3.getId(), p2.getId(), p1.getId());
        }
    }
}
