package com.gentlemanstore.inventory.repository;

import com.gentlemanstore.inventory.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findAllByResolvedFalseAndDeletedFalse();
    Optional<StockAlert> findByIdAndDeletedFalse(Long id);
}
