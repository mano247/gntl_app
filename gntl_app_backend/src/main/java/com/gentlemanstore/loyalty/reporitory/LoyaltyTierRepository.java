package com.gentlemanstore.loyalty.reporitory;

import com.gentlemanstore.loyalty.model.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, Long> {
    Optional<LoyaltyTier> findTopByMinPointsLessThanEqualOrderByMinPointsDesc(Integer points);
}
