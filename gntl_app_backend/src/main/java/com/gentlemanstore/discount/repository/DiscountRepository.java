package com.gentlemanstore.discount.repository;

import com.gentlemanstore.discount.model.Discount;
import com.gentlemanstore.discount.model.DiscountScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findAllByDeletedFalse();

    // CATEGORY popust ima prioritet nad GLOBAL (specificniji pobedjuje);
    // LEFT JOIN je obavezan da implicitni join po kategoriji ne bi izbacio GLOBAL redove.
    @Query("SELECT d FROM Discount d LEFT JOIN d.category c WHERE d.deleted = false " +
            "AND d.validFrom <= :now AND d.validTo >= :now " +
            "AND (d.scope = com.gentlemanstore.discount.model.DiscountScope.GLOBAL " +
            "     OR (d.scope = com.gentlemanstore.discount.model.DiscountScope.CATEGORY AND c.id = :categoryId)) " +
            "ORDER BY CASE WHEN d.scope = com.gentlemanstore.discount.model.DiscountScope.CATEGORY THEN 0 ELSE 1 END, d.id DESC")
    List<Discount> findActiveDiscountsForCategory(
            @Param("categoryId") Long categoryId,
            @Param("now") LocalDateTime now
    );

    default Optional<Discount> findActiveDiscountForProduct(Long categoryId, LocalDateTime now) {
        return findActiveDiscountsForCategory(categoryId, now).stream().findFirst();
    }
}
