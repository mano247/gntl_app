package com.gentlemanstore.order.repository;

import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    Page<Order> findAllByUserIdAndStatusAndDeletedFalse(Long userId, OrderStatus status, Pageable pageable);
    Optional<Order> findByIdAndDeletedFalse(Long id);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false")
    Integer countTotalOrders();

    @Query("SELECT AVG(o.totalPrice) FROM Order o WHERE o.deleted = false")
    BigDecimal getAverageOrderValue();

    @Query("SELECT MONTH(o.createdAt) as month, YEAR(o.createdAt) as year, SUM(o.totalPrice) as revenue FROM Order o WHERE o.deleted = false GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenue();

    // Agregati za jedan kalendarski mesec [from, to) — mesečni izveštaji i
    // current-month dashboard.
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false AND o.createdAt >= :from AND o.createdAt < :to")
    Integer countOrdersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.deleted = false AND o.createdAt >= :from AND o.createdAt < :to")
    BigDecimal getRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT AVG(o.totalPrice) FROM Order o WHERE o.deleted = false AND o.createdAt >= :from AND o.createdAt < :to")
    BigDecimal getAverageOrderValueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT MIN(o.createdAt) FROM Order o WHERE o.deleted = false")
    LocalDateTime getEarliestOrderDate();

    // EntityGraph na user: mapper sada mapira customerName/customerEmail,
    // bez ovoga bi svaki red stranice okidao poseban SELECT za usera (N+1).
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAllByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAllByStatusAndDeletedFalse(OrderStatus status, Pageable pageable);
}
