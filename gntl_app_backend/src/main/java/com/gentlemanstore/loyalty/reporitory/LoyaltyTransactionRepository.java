package com.gentlemanstore.loyalty.reporitory;

import com.gentlemanstore.loyalty.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    List<LoyaltyTransaction> findAllByLoyaltyAccountIdAndDeletedFalse(Long accountId);
}
