package com.gentlemanstore.support.repository;

import com.gentlemanstore.support.model.SupportTicket;
import com.gentlemanstore.support.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findAllByDeletedFalse(Pageable pageable);
    Page<SupportTicket> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    List<SupportTicket> findAllByStatusAndDeletedFalse(TicketStatus status);
    Optional<SupportTicket> findByChatSessionId(Long sessionId);
    List<SupportTicket> findAllByUserIdAndDeletedFalse(Long userId);
}
