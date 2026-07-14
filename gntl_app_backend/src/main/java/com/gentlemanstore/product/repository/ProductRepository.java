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

    // Bez deleted filtera - skup id-jeva vec dolazi filtriran iz findIdsByFilters,
    // pa isti upit radi i za ACTIVE i za DELETED/ALL prikaz (staff).
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images LEFT JOIN FETCH p.sizes LEFT JOIN FETCH p.tags LEFT JOIN FETCH p.category WHERE p.id IN :ids")
    List<Product> findAllByIdInWithDetails(@Param("ids") List<Long> ids);

    @Query("SELECT p.id FROM Product p WHERE p.deleted = false")
    Page<Long> findIdsByDeletedFalse(Pageable pageable);

    // includeActive/includeDeleted umesto nullable Boolean parametra - izbegava
    // Postgres problem sa tipom NULL parametra u "x IS NULL OR ..." obrascu.
    @Query("SELECT p.id FROM Product p " +
            "LEFT JOIN p.category c " +
            "WHERE ((:includeActive = true AND p.deleted = false) " +
            "   OR (:includeDeleted = true AND p.deleted = true)) " +
            "AND (:category IS NULL OR LOWER(c.name) = LOWER(CAST(:category AS string))) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Long> findIdsByFilters(
            @Param("category") String category,
            @Param("search") String search,
            @Param("includeActive") boolean includeActive,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);
}
