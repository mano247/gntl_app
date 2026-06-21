package com.gentlemanstore.discount.repository;

import com.gentlemanstore.discount.model.UserDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDiscountRepository extends JpaRepository<UserDiscount, Long> {
    boolean existsByUserIdAndDiscountIdAndDeletedFalse(Long userId, Long discountId);
}