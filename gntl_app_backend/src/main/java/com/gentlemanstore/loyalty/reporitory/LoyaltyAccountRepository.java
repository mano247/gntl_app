package com.gentlemanstore.loyalty.reporitory;

import com.gentlemanstore.loyalty.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByUserIdAndDeletedFalse(Long userId);
}
