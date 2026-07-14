package com.gentlemanstore.discount.repository;

import com.gentlemanstore.discount.model.UserPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Long> {
    List<UserPromotion> findAllByUserIdAndDeletedFalse(Long userId);
    boolean existsByUserIdAndPromotionIdAndDeletedFalse(Long userId, Long promotionId);
}
