package com.gentlemanstore.order.repository;

import com.gentlemanstore.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserIdAndDeletedFalse(Long userId);
    Optional<Order> findByIdAndDeletedFalse(Long id);
}
