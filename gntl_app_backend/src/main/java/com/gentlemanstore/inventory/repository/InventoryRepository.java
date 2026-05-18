package com.gentlemanstore.inventory.repository;

import com.gentlemanstore.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductSizeIdAndDeletedFalse(Long productSizeId);
    List<Inventory> findAllByDeletedFalse();
}
