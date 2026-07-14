package com.gentlemanstore.discount.repository;

import com.gentlemanstore.discount.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAllByDeletedFalse();
    List<Promotion> findAllByActiveTrueAndValidFromBeforeAndValidToAfterAndDeletedFalse(
            LocalDateTime now1, LocalDateTime now2);
    Optional<Promotion> findByCodeAndDeletedFalse(String code);
    boolean existsByCode(String code);
}
