package com.gentlemanstore.support.repository;

import com.gentlemanstore.support.model.SupportTicket;
import com.gentlemanstore.support.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findAllByDeletedFalse();
    List<SupportTicket> findAllByUserIdAndDeletedFalse(Long userId);
    List<SupportTicket> findAllByStatusAndDeletedFalse(TicketStatus status);
}
