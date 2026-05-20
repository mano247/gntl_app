package com.gentlemanstore.order.repository;

import com.gentlemanstore.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    Optional<Order> findByIdAndDeletedFalse(Long id);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false")
    Integer countTotalOrders();

    @Query("SELECT AVG(o.totalPrice) FROM Order o WHERE o.deleted = false")
    BigDecimal getAverageOrderValue();

    @Query("SELECT MONTH(o.createdAt) as month, YEAR(o.createdAt) as year, SUM(o.totalPrice) as revenue FROM Order o WHERE o.deleted = false GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenue();
}
