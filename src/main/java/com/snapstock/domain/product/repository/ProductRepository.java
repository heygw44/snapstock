package com.snapstock.domain.product.repository;

import com.snapstock.domain.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> findByCategoryAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(
            String category, Long id, Pageable pageable);

    List<Product> findByCategoryAndDeletedAtIsNullOrderByIdDesc(
            String category, Pageable pageable);

    List<Product> findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(
            Long id, Pageable pageable);

    List<Product> findByDeletedAtIsNullOrderByIdDesc(Pageable pageable);
}
