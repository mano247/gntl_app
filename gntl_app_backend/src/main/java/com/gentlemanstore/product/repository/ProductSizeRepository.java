package com.gentlemanstore.product.repository;

import com.gentlemanstore.product.model.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    Optional<ProductSize> findByIdAndDeletedFalse(Long id);
}
