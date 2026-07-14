package com.gentlemanstore.support.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.support.dto.ChatMessageDTO;
import com.gentlemanstore.support.dto.SupportTicketDTO;
import com.gentlemanstore.support.mapper.SupportMapper;
import com.gentlemanstore.support.model.*;
import com.gentlemanstore.support.repository.*;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportServiceTest {

    @Mock
    private BotQuestionRepository botQuestionRepository;

    @Mock
    private BotResponseRepository botResponseRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupportMapper mapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SupportService supportService;

    private SupportTicket ticket(long id, TicketStatus status, boolean archived) {
        User owner = new User();
        owner.setId(10L);
        ChatSession session = ChatSession.builder().id(100L).deleted(false).build();
        return SupportTicket.builder()
                .id(id)
                .subject("Subject")
                .status(status)
                .user(owner)
                .chatSession(session)
                .deleted(false)
                .archivedByStaff(archived)
                .build();
    }

    // ---------- archiveTicket (employee "delete" iz staff liste) ----------

    @Test
    void archiveTicketSetsFlagWithoutDeleting() {
        SupportTicket ticket = ticket(1L, TicketStatus.OPEN, false);
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        supportService.archiveTicket(1L);

        assertTrue(ticket.isArchivedByStaff(), "tiket se arhivira za staff listu");
        assertFalse(ticket.isDeleted(), "tiket NIJE obrisan - customer i dalje vidi istoriju");
        verify(supportTicketRepository).save(ticket);
    }

    @Test
    void archiveTicketNotFoundThrows404() {
        when(supportTicketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supportService.archiveTicket(99L));
    }

    @Test
    void archiveDeletedTicketThrows404() {
        SupportTicket ticket = ticket(1L, TicketStatus.OPEN, false);
        ticket.setDeleted(true);
        when(supportTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(ResourceNotFoundException.class, () -> supportService.archiveTicket(1L));
        verify(supportTicketRepository, never()).save(any());
    }

    @Test
    void staffListExcludesArchivedTickets() {
        Pageable pageable = Pageable.unpaged();
        when(supportTicketRepository.findAllByDeletedFalseAndArchivedByStaffFalse(pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        supportService.getAllTickets(pageable);

        verify(supportTicketRepository).findAllByDeletedFalseAndArchivedByStaffFalse(pageable);
        verify(supportTicketRepository, never()).findAllByDeletedFalse(pageable);
    }

    // ---------- unread summary po statusu (badge kategorije) ----------

    @Test
    void myUnreadSummaryCountsEmployeeMessagesAndDefaultsToZero() {
        when(supportTicketRepository.countUnreadMessagesByStatusForUser(10L, MessageSender.EMPLOYEE))
                .thenReturn(List.<Object[]>of(
                        new Object[]{TicketStatus.OPEN, 3L},
                        new Object[]{TicketStatus.RESOLVED, 1L}));

        Map<String, Integer> summary = supportService.getMyUnreadSummary(10L);

        assertEquals(3, summary.get("OPEN"));
        assertEquals(1, summary.get("RESOLVED"));
        assertEquals(0, summary.get("IN_PROGRESS"), "kategorija bez unread poruka je 0");
        assertEquals(0, summary.get("CLOSED"));
        assertEquals(4, summary.values().stream().mapToInt(Integer::intValue).sum(),
                "ALL badge = zbir svih kategorija");
    }

    @Test
    void staffUnreadSummaryCountsUserMessages() {
        when(supportTicketRepository.countUnreadMessagesByStatusForStaff(MessageSender.USER))
                .thenReturn(List.<Object[]>of(new Object[]{TicketStatus.IN_PROGRESS, 7L}));

        Map<String, Integer> summary = supportService.getStaffUnreadSummary();

        assertEquals(7, summary.get("IN_PROGRESS"));
        assertEquals(0, summary.get("OPEN"));
        verify(supportTicketRepository).countUnreadMessagesByStatusForStaff(MessageSender.USER);
    }

    // ---------- nova customer poruka vraca arhiviran tiket u staff listu ----------

    @Test
    void userMessageUnarchivesTicketForStaff() {
        SupportTicket ticket = ticket(1L, TicketStatus.OPEN, true);
        User owner = ticket.getUser();
        when(supportTicketRepository.findByChatSessionId(100L)).thenReturn(Optional.of(ticket));
        when(chatSessionRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(ticket.getChatSession()));
        when(mapper.toMessageDTO(any())).thenReturn(new ChatMessageDTO());
        when(mapper.toDTO(ticket)).thenReturn(new SupportTicketDTO());

        supportService.sendMessage(100L, "Hello again", "USER", owner);

        assertFalse(ticket.isArchivedByStaff(), "customer poruka vraca tiket u staff listu");
        verify(messagingTemplate).convertAndSend(eq("/topic/employee/new-ticket"), any(SupportTicketDTO.class));
    }

    @Test
    void employeeMessageDoesNotUnarchiveTicket() {
        SupportTicket ticket = ticket(1L, TicketStatus.OPEN, true);
        User owner = ticket.getUser();
        when(supportTicketRepository.findByChatSessionId(100L)).thenReturn(Optional.of(ticket));
        when(chatSessionRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(ticket.getChatSession()));
        when(mapper.toMessageDTO(any())).thenReturn(new ChatMessageDTO());

        supportService.sendMessage(100L, "Staff reply", "EMPLOYEE", owner);

        assertTrue(ticket.isArchivedByStaff(), "employee poruka ne menja arhivirano stanje");
    }
}
