package com.snapstock.domain.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapstock.domain.product.dto.ProductCreateRequest;
import com.snapstock.domain.product.dto.ProductResponse;
import com.snapstock.domain.product.dto.ProductUpdateRequest;
import com.snapstock.domain.product.service.ProductService;
import com.snapstock.domain.user.entity.Role;
import com.snapstock.global.auth.ApiAccessDeniedHandler;
import com.snapstock.global.auth.ApiAuthenticationEntryPoint;
import com.snapstock.global.auth.JwtAuthenticationFilter;
import com.snapstock.global.auth.JwtTokenProvider;
import com.snapstock.global.auth.TokenRedisService;
import com.snapstock.global.config.SecurityConfig;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
class AdminProductControllerTest {

    private static final String ADMIN_PRODUCTS_URL = "/api/v1/admin/products";
    private static final String ADMIN_TOKEN = "admin-access-token";
    private static final String USER_TOKEN = "user-access-token";
    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisService tokenRedisService;

    private void setupAdminAuth() {
        given(jwtTokenProvider.resolveToken(any())).willReturn(ADMIN_TOKEN);
        given(jwtTokenProvider.validateToken(ADMIN_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUserId(ADMIN_TOKEN)).willReturn(ADMIN_ID);
        given(jwtTokenProvider.getRole(ADMIN_TOKEN)).willReturn(Role.ADMIN);
        given(tokenRedisService.isBlacklisted(ADMIN_TOKEN)).willReturn(false);
    }

    private void setupUserAuth() {
        given(jwtTokenProvider.resolveToken(any())).willReturn(USER_TOKEN);
        given(jwtTokenProvider.validateToken(USER_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUserId(USER_TOKEN)).willReturn(USER_ID);
        given(jwtTokenProvider.getRole(USER_TOKEN)).willReturn(Role.USER);
        given(tokenRedisService.isBlacklisted(USER_TOKEN)).willReturn(false);
    }

    @Nested
    class Create {

        @Test
        void create_ADMIN권한_정상요청_201응답() throws Exception {
            // given
            setupAdminAuth();
            ProductCreateRequest request = new ProductCreateRequest(
                    "테스트 상품", "설명", 10000, 100, "전자기기");
            ProductResponse response = new ProductResponse(
                    1L, "테스트 상품", "설명", 10000, 100, "전자기기", LocalDateTime.now());

            given(productService.createProduct(any(ProductCreateRequest.class)))
                    .willReturn(response);

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.productId").value(1L))
                    .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                    .andExpect(jsonPath("$.data.description").value("설명"))
                    .andExpect(jsonPath("$.data.originalPrice").value(10000))
                    .andExpect(jsonPath("$.data.stock").value(100))
                    .andExpect(jsonPath("$.data.category").value("전자기기"));
        }

        @Test
        void create_USER권한_403응답() throws Exception {
            // given
            setupUserAuth();
            ProductCreateRequest request = new ProductCreateRequest(
                    "테스트 상품", "설명", 10000, 100, "전자기기");

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + USER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }

        @Test
        void create_미인증_401응답() throws Exception {
            // given
            ProductCreateRequest request = new ProductCreateRequest(
                    "테스트 상품", "설명", 10000, 100, "전자기기");

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        }

        @Test
        void create_상품명빈값_400응답() throws Exception {
            // given
            setupAdminAuth();
            ProductCreateRequest request = new ProductCreateRequest(
                    "", "설명", 10000, 100, "전자기기");

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
        }

        @Test
        void create_가격0이하_400응답() throws Exception {
            // given
            setupAdminAuth();
            ProductCreateRequest request = new ProductCreateRequest(
                    "테스트 상품", "설명", 0, 100, "전자기기");

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'originalPrice')]").exists());
        }

        @Test
        void create_재고음수_400응답() throws Exception {
            // given
            setupAdminAuth();
            ProductCreateRequest request = new ProductCreateRequest(
                    "테스트 상품", "설명", 10000, -1, "전자기기");

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'stock')]").exists());
        }

        @Test
        void create_가격누락_400응답() throws Exception {
            // given
            setupAdminAuth();
            String jsonWithoutPrice = """
                    {"name": "테스트 상품", "description": "설명", "stock": 100, "category": "전자기기"}
                    """;

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonWithoutPrice));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'originalPrice')]").exists());
        }

        @Test
        void create_카테고리빈값_400응답() throws Exception {
            // given
            setupAdminAuth();
            ProductCreateRequest request = new ProductCreateRequest(
                    "테스트 상품", "설명", 10000, 100, "");

            // when
            var result = mockMvc.perform(post(ADMIN_PRODUCTS_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'category')]").exists());
        }
    }

    @Nested
    class Delete {

        private static final Long PRODUCT_ID = 1L;
        private static final String DELETE_URL = ADMIN_PRODUCTS_URL + "/" + PRODUCT_ID;

        @Test
        void delete_ADMIN권한_정상요청_204응답() throws Exception {
            // given
            setupAdminAuth();
            willDoNothing().given(productService).deleteProduct(PRODUCT_ID);

            // when
            var result = mockMvc.perform(delete(DELETE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN));

            // then
            result.andExpect(status().isNoContent());
        }

        @Test
        void delete_USER권한_403응답() throws Exception {
            // given
            setupUserAuth();

            // when
            var result = mockMvc.perform(delete(DELETE_URL)
                    .header("Authorization", "Bearer " + USER_TOKEN));

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }

        @Test
        void delete_미인증_401응답() throws Exception {
            // when
            var result = mockMvc.perform(delete(DELETE_URL));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        }

        @Test
        void delete_상품없음_404응답() throws Exception {
            // given
            setupAdminAuth();
            willThrow(new CustomException(ErrorCode.PRODUCT_NOT_FOUND))
                    .given(productService).deleteProduct(PRODUCT_ID);

            // when
            var result = mockMvc.perform(delete(DELETE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN));

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        void delete_진행중인타임딜존재_409응답() throws Exception {
            // given
            setupAdminAuth();
            willThrow(new CustomException(ErrorCode.PRODUCT_HAS_ACTIVE_DEAL))
                    .given(productService).deleteProduct(PRODUCT_ID);

            // when
            var result = mockMvc.perform(delete(DELETE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN));

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_HAS_ACTIVE_DEAL"));
        }
    }

    @Nested
    class Update {

        private static final Long PRODUCT_ID = 1L;
        private static final String UPDATE_URL = ADMIN_PRODUCTS_URL + "/" + PRODUCT_ID;

        @Test
        void update_ADMIN권한_정상요청_200응답() throws Exception {
            // given
            setupAdminAuth();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "수정 상품", null, 20000, null, null);
            ProductResponse response = new ProductResponse(
                    PRODUCT_ID, "수정 상품", "설명", 20000, 100, "전자기기", LocalDateTime.now());

            given(productService.updateProduct(eq(PRODUCT_ID), any(ProductUpdateRequest.class)))
                    .willReturn(response);

            // when
            var result = mockMvc.perform(patch(UPDATE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
                    .andExpect(jsonPath("$.data.name").value("수정 상품"))
                    .andExpect(jsonPath("$.data.originalPrice").value(20000));
        }

        @Test
        void update_USER권한_403응답() throws Exception {
            // given
            setupUserAuth();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "수정 상품", null, null, null, null);

            // when
            var result = mockMvc.perform(patch(UPDATE_URL)
                    .header("Authorization", "Bearer " + USER_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }

        @Test
        void update_미인증_401응답() throws Exception {
            // given
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "수정 상품", null, null, null, null);

            // when
            var result = mockMvc.perform(patch(UPDATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        }

        @Test
        void update_상품없음_404응답() throws Exception {
            // given
            setupAdminAuth();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "수정 상품", null, null, null, null);

            given(productService.updateProduct(eq(PRODUCT_ID), any(ProductUpdateRequest.class)))
                    .willThrow(new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            // when
            var result = mockMvc.perform(patch(UPDATE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        void update_가격음수_400응답() throws Exception {
            // given
            setupAdminAuth();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    null, null, -1000, null, null);

            // when
            var result = mockMvc.perform(patch(UPDATE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'originalPrice')]").exists());
        }

        @Test
        void update_빈바디_400응답() throws Exception {
            // given
            setupAdminAuth();
            String emptyJson = "{}";

            given(productService.updateProduct(eq(PRODUCT_ID), any(ProductUpdateRequest.class)))
                    .willThrow(new CustomException(ErrorCode.INVALID_INPUT));

            // when
            var result = mockMvc.perform(patch(UPDATE_URL)
                    .header("Authorization", "Bearer " + ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyJson));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        }
    }
}
