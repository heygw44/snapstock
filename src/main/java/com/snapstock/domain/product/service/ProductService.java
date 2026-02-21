package com.snapstock.domain.product.service;

import com.snapstock.domain.product.dto.ProductCreateRequest;
import com.snapstock.domain.product.dto.ProductResponse;
import com.snapstock.domain.product.dto.ProductUpdateRequest;
import com.snapstock.domain.product.entity.Product;
import com.snapstock.domain.product.entity.ProductCommand;
import com.snapstock.domain.product.repository.ProductRepository;
import com.snapstock.global.error.CustomException;
import com.snapstock.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        ProductCommand command = new ProductCommand(
                request.name(), request.description(),
                request.originalPrice(), request.stock(), request.category());
        Product product = safeDomainCreate(command);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        validateAtLeastOneFieldProvided(request);
        Product product = findActiveProduct(productId);
        applyUpdates(product, request);
        return ProductResponse.from(product);
    }

    private Product findActiveProduct(Long productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateAtLeastOneFieldProvided(ProductUpdateRequest request) {
        if (!request.hasUpdatableField()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void applyUpdates(Product product, ProductUpdateRequest request) {
        validateNameIfPresent(request.name());
        validateCategoryIfPresent(request.category());
        ProductCommand command = new ProductCommand(
                resolveOrKeep(request.name(), product.getName()),
                resolveOrKeep(request.description(), product.getDescription()),
                resolveOrKeep(request.originalPrice(), product.getOriginalPrice()),
                resolveOrKeep(request.stock(), product.getStock()),
                resolveOrKeep(request.category(), product.getCategory()));
        safeDomainUpdate(product, command);
    }

    private void validateNameIfPresent(String name) {
        if (name == null) { return; }
        if (name.isBlank()) { throw new CustomException(ErrorCode.INVALID_INPUT); }
        if (name.length() > Product.NAME_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateCategoryIfPresent(String category) {
        if (category == null) { return; }
        if (category.isBlank()) { throw new CustomException(ErrorCode.INVALID_INPUT); }
        if (category.length() > Product.CATEGORY_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private Product safeDomainCreate(ProductCommand command) {
        try {
            return Product.create(command);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void safeDomainUpdate(Product product, ProductCommand command) {
        try {
            product.update(command);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private <T> T resolveOrKeep(T newValue, T currentValue) {
        return newValue != null ? newValue : currentValue;
    }
}
