package com.gentlemanstore.order.repository;

import com.gentlemanstore.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi.product.name, SUM(oi.quantity) as totalSold FROM OrderItem oi WHERE oi.deleted = false GROUP BY oi.product.name ORDER BY totalSold DESC")
    List<Object[]> getTopProducts(Pageable pageable);

    // Top proizvodi za jedan kalendarski mesec [from, to) — snapshot za
    // mesečne izveštaje i current-month dashboard.
    @Query("SELECT oi.product.name, SUM(oi.quantity) as totalSold FROM OrderItem oi " +
            "WHERE oi.deleted = false AND oi.order.deleted = false " +
            "AND oi.order.createdAt >= :from AND oi.order.createdAt < :to " +
            "GROUP BY oi.product.name ORDER BY totalSold DESC")
    List<Object[]> getTopProductsBetween(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         Pageable pageable);
}
