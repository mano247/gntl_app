package com.gentlemanstore.discount.repository;

import com.gentlemanstore.discount.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    Optional<Discount> findByCodeAndDeletedFalse(String code);
    List<Discount> findAllByDeletedFalse();

    @Query("SELECT d FROM Discount d WHERE d.deleted = false " +
            "AND d.validFrom <= :now AND d.validTo >= :now " +
            "AND (d.product.id = :productId OR d.category.id = :categoryId) " +
            "ORDER BY d.product.id DESC")
    Optional<Discount> findActiveDiscountForProduct(
            @Param("productId") Long productId,
            @Param("categoryId") Long categoryId,
            @Param("now") LocalDateTime now
    );
}
