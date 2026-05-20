package com.gentlemanstore.payment.repository;

import com.gentlemanstore.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderIdAndDeletedFalse(Long orderId);
    Page<Payment> findAllByDeletedFalse(Pageable pageable);
}
