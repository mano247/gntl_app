package com.gentlemanstore.support.repository;

import com.gentlemanstore.support.model.MessageSender;
import com.gentlemanstore.support.model.SupportTicket;
import com.gentlemanstore.support.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findAllByDeletedFalse(Pageable pageable);
    Page<SupportTicket> findAllByDeletedFalseAndArchivedByStaffFalse(Pageable pageable);
    Page<SupportTicket> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    List<SupportTicket> findAllByStatusAndDeletedFalse(TicketStatus status);
    Optional<SupportTicket> findByChatSessionId(Long sessionId);
    List<SupportTicket> findAllByUserIdAndDeletedFalse(Long userId);

    // Unread poruke grupisane po statusu tiketa - izvor za badge brojace na
    // status filter chipovima (racuna se nad SVIM tiketima, ne nad jednom
    // paginiranom stranicom).
    @Query("SELECT t.status, COUNT(m) FROM SupportTicket t " +
            "JOIN ChatMessage m ON m.chatSession.id = t.chatSession.id " +
            "WHERE t.deleted = false AND t.user.id = :userId " +
            "AND m.deleted = false AND m.isRead = false AND m.sender = :sender " +
            "GROUP BY t.status")
    List<Object[]> countUnreadMessagesByStatusForUser(@Param("userId") Long userId,
                                                      @Param("sender") MessageSender sender);

    @Query("SELECT t.status, COUNT(m) FROM SupportTicket t " +
            "JOIN ChatMessage m ON m.chatSession.id = t.chatSession.id " +
            "WHERE t.deleted = false AND t.archivedByStaff = false " +
            "AND m.deleted = false AND m.isRead = false AND m.sender = :sender " +
            "GROUP BY t.status")
    List<Object[]> countUnreadMessagesByStatusForStaff(@Param("sender") MessageSender sender);
}
