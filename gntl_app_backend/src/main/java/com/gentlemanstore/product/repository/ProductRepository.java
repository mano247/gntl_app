package com.gentlemanstore.product.repository;

import com.gentlemanstore.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByDeletedFalse(Pageable pageable);
    Optional<Product> findByIdAndDeletedFalse(Long id);
    boolean existsBySku(String sku);
}
