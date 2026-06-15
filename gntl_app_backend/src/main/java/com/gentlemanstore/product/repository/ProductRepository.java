package com.gentlemanstore.product.repository;

import com.gentlemanstore.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByDeletedFalse(Pageable pageable);
    Optional<Product> findByIdAndDeletedFalse(Long id);
    boolean existsBySku(String sku);
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images LEFT JOIN FETCH p.sizes LEFT JOIN FETCH p.tags LEFT JOIN FETCH p.category WHERE p.id IN :ids AND p.deleted = false")
    List<Product> findAllByIdInWithDetails(@Param("ids") List<Long> ids);
    @Query("SELECT p.id FROM Product p WHERE p.deleted = false")
    Page<Long> findIdsByDeletedFalse(Pageable pageable);
}
