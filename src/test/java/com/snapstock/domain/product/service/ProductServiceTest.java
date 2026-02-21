package com.snapstock.domain.product.service;

import com.snapstock.domain.product.dto.ProductCreateRequest;
import com.snapstock.domain.product.dto.ProductResponse;
import com.snapstock.domain.product.dto.ProductUpdateRequest;
import com.snapstock.domain.product.entity.Product;
import com.snapstock.domain.product.entity.ProductCommand;
import com.snapstock.domain.product.repository.ProductRepository;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final String PRODUCT_NAME = "테스트 상품";
    private static final String PRODUCT_DESCRIPTION = "테스트 설명";
    private static final int ORIGINAL_PRICE = 10000;
    private static final int STOCK = 100;
    private static final String CATEGORY = "전자기기";

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product createTestProduct() {
        Product product = Product.create(new ProductCommand(
                PRODUCT_NAME, PRODUCT_DESCRIPTION, ORIGINAL_PRICE, STOCK, CATEGORY));
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());
        return product;
    }

    @Nested
    class CreateProduct {

        @Test
        void createProduct_정상요청_상품생성() {
            // given
            ProductCreateRequest request = new ProductCreateRequest(
                    PRODUCT_NAME, PRODUCT_DESCRIPTION, ORIGINAL_PRICE, STOCK, CATEGORY);
            Product saved = createTestProduct();

            given(productRepository.save(any(Product.class))).willReturn(saved);

            // when
            ProductResponse response = productService.createProduct(request);

            // then
            assertThat(response.productId()).isEqualTo(PRODUCT_ID);
            assertThat(response.name()).isEqualTo(PRODUCT_NAME);
            assertThat(response.description()).isEqualTo(PRODUCT_DESCRIPTION);
            assertThat(response.originalPrice()).isEqualTo(ORIGINAL_PRICE);
            assertThat(response.stock()).isEqualTo(STOCK);
            assertThat(response.category()).isEqualTo(CATEGORY);
            then(productRepository).should().save(any(Product.class));
        }

        @Test
        void createProduct_가격이_0이면_예외발생() {
            // given
            ProductCreateRequest request = new ProductCreateRequest(
                    PRODUCT_NAME, PRODUCT_DESCRIPTION, 0, STOCK, CATEGORY);

            // when & then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    @Nested
    class UpdateProduct {

        @Test
        void updateProduct_이름만변경_나머지유지() {
            // given
            Product product = createTestProduct();
            String newName = "변경된 상품명";
            ProductUpdateRequest request = new ProductUpdateRequest(
                    newName, null, null, null, null);

            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                    .willReturn(Optional.of(product));

            // when
            ProductResponse response = productService.updateProduct(PRODUCT_ID, request);

            // then
            assertThat(response.name()).isEqualTo(newName);
            assertThat(response.description()).isEqualTo(PRODUCT_DESCRIPTION);
            assertThat(response.originalPrice()).isEqualTo(ORIGINAL_PRICE);
            assertThat(response.stock()).isEqualTo(STOCK);
            assertThat(response.category()).isEqualTo(CATEGORY);
        }

        @Test
        void updateProduct_모든필드변경() {
            // given
            Product product = createTestProduct();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "새 이름", "새 설명", 20000, 200, "의류");

            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                    .willReturn(Optional.of(product));

            // when
            ProductResponse response = productService.updateProduct(PRODUCT_ID, request);

            // then
            assertThat(response.name()).isEqualTo("새 이름");
            assertThat(response.description()).isEqualTo("새 설명");
            assertThat(response.originalPrice()).isEqualTo(20000);
            assertThat(response.stock()).isEqualTo(200);
            assertThat(response.category()).isEqualTo("의류");
        }

        @Test
        void updateProduct_상품없음_예외발생() {
            // given
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "새 이름", null, null, null, null);

            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(PRODUCT_ID, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
        }

        @Test
        void updateProduct_빈바디_예외발생() {
            // given
            ProductUpdateRequest request = new ProductUpdateRequest(
                    null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(PRODUCT_ID, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        void updateProduct_이름빈값_예외발생() {
            // given
            Product product = createTestProduct();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "  ", null, null, null, null);

            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
                    .willReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(PRODUCT_ID, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }
}
