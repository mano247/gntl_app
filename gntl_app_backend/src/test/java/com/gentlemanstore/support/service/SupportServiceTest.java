package com.gentlemanstore.support.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.support.dto.ChatMessageDTO;
import com.gentlemanstore.support.dto.CreateTicketRequest;
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
    private OrderRepository orderRepository;

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

    // ---------- kreiranje tiketa: povezani order + strukturisana hitnost ----------

    private User customer(long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Petar");
        user.setLastName("Petrović");
        user.setEmail("petar@example.com");
        return user;
    }

    private Order order(long id, User owner, boolean deleted) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setDeleted(deleted);
        return order;
    }

    @Test
    void createTicketStoresLinkedOrderAndUrgency() {
        User owner = customer(10L);
        Order order = order(38L, owner, false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(orderRepository.findByIdAndDeletedFalse(38L)).thenReturn(Optional.of(order));
        when(mapper.toDTO(any(SupportTicket.class))).thenReturn(new SupportTicketDTO());

        CreateTicketRequest request = CreateTicketRequest.builder()
                .subject("Problem sa porudžbinom")
                .orderId(38L)
                .urgency("HIGH")
                .build();

        supportService.createTicket(10L, request);

        org.mockito.ArgumentCaptor<SupportTicket> captor =
                org.mockito.ArgumentCaptor.forClass(SupportTicket.class);
        verify(supportTicketRepository).save(captor.capture());
        assertEquals(TicketUrgency.HIGH, captor.getValue().getUrgency());
        assertEquals(38L, captor.getValue().getOrder().getId());
    }

    @Test
    void createTicketWithoutOrderIsAllowed() {
        User owner = customer(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(mapper.toDTO(any(SupportTicket.class))).thenReturn(new SupportTicketDTO());

        CreateTicketRequest request = CreateTicketRequest.builder()
                .subject("Opšte pitanje")
                .urgency("LOW")
                .build();

        supportService.createTicket(10L, request);

        org.mockito.ArgumentCaptor<SupportTicket> captor =
                org.mockito.ArgumentCaptor.forClass(SupportTicket.class);
        verify(supportTicketRepository).save(captor.capture());
        assertNull(captor.getValue().getOrder(), "tiket bez porudžbine je validan");
        assertEquals(TicketUrgency.LOW, captor.getValue().getUrgency());
        verify(orderRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void createTicketDefaultsUrgencyToMediumWhenMissing() {
        User owner = customer(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(mapper.toDTO(any(SupportTicket.class))).thenReturn(new SupportTicketDTO());

        CreateTicketRequest request = CreateTicketRequest.builder()
                .subject("Bez hitnosti")
                .build();

        supportService.createTicket(10L, request);

        org.mockito.ArgumentCaptor<SupportTicket> captor =
                org.mockito.ArgumentCaptor.forClass(SupportTicket.class);
        verify(supportTicketRepository).save(captor.capture());
        assertEquals(TicketUrgency.MEDIUM, captor.getValue().getUrgency());
    }

    @Test
    void createTicketRejectsInvalidUrgency() {
        User owner = customer(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

        CreateTicketRequest request = CreateTicketRequest.builder()
                .subject("Nevalidna hitnost")
                .urgency("SUPER_URGENT")
                .build();

        assertThrows(BadRequestException.class, () -> supportService.createTicket(10L, request));
        verify(supportTicketRepository, never()).save(any());
    }

    @Test
    void createTicketRejectsForeignOrder() {
        User owner = customer(10L);
        User otherUser = customer(99L);
        Order foreignOrder = order(38L, otherUser, false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(orderRepository.findByIdAndDeletedFalse(38L)).thenReturn(Optional.of(foreignOrder));

        CreateTicketRequest request = CreateTicketRequest.builder()
                .subject("Tuđa porudžbina")
                .orderId(38L)
                .urgency("MEDIUM")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> supportService.createTicket(10L, request),
                "tuđa porudžbina se ne sme povezati (404, ne 403)");
        verify(supportTicketRepository, never()).save(any());
    }

    @Test
    void createTicketRejectsDeletedOrMissingOrder() {
        User owner = customer(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(orderRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        CreateTicketRequest request = CreateTicketRequest.builder()
                .subject("Nepostojeća porudžbina")
                .orderId(404L)
                .urgency("MEDIUM")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> supportService.createTicket(10L, request));
        verify(supportTicketRepository, never()).save(any());
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
