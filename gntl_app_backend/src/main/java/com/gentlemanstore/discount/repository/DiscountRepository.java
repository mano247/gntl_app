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
            "ORDER BY CASE WHEN d.product.id IS NOT NULL THEN 0 ELSE 1 END, d.id DESC")
    List<Discount> findActiveDiscountsForProduct(
            @Param("productId") Long productId,
            @Param("categoryId") Long categoryId,
            @Param("now") LocalDateTime now
    );

    default Optional<Discount> findActiveDiscountForProduct(Long productId, Long categoryId, LocalDateTime now) {
        return findActiveDiscountsForProduct(productId, categoryId, now).stream().findFirst();
    }
}
