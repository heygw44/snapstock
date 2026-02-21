package com.snapstock.domain.product.service;

import com.snapstock.domain.product.dto.ProductCreateRequest;
import com.snapstock.domain.product.dto.ProductResponse;
import com.snapstock.domain.product.entity.Product;
import com.snapstock.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
        Product product = Product.create(
                PRODUCT_NAME, PRODUCT_DESCRIPTION, ORIGINAL_PRICE, STOCK, CATEGORY);
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
    }
}
