package com.gentlemanstore.support.repository;

import com.gentlemanstore.support.model.BotResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotResponseRepository extends JpaRepository<BotResponse, Long> {
    List<BotResponse> findAllBySupportTicketIdAndDeletedFalse(Long ticketId);
}
