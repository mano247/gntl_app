package com.gentlemanstore.support.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.support.dto.*;
import com.gentlemanstore.support.mapper.SupportMapper;
import com.gentlemanstore.support.model.*;
import com.gentlemanstore.support.repository.*;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final BotQuestionRepository botQuestionRepository;
    private final BotResponseRepository botResponseRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SupportMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public SupportTicketDTO createTicket(Long userId, CreateTicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TicketUrgency urgency = parseUrgency(request.getUrgency());
        Order linkedOrder = resolveLinkedOrder(request.getOrderId(), userId);

        ChatSession chatSession = ChatSession.builder()
                .deleted(false)
                .build();
        chatSessionRepository.save(chatSession);

        SupportTicket ticket = SupportTicket.builder()
                .subject(request.getSubject())
                .status(TicketStatus.OPEN)
                .urgency(urgency)
                .order(linkedOrder)
                .user(user)
                .chatSession(chatSession)
                .deleted(false)
                .build();

        supportTicketRepository.save(ticket);

        ChatMessage welcomeMessage = ChatMessage.builder()
                .content("Thank you for reaching out! Your support request has been received and a ticket has been created. One of our team members will get back to you here in the chat shortly.")
                .sender(MessageSender.BOT)
                .chatSession(chatSession)
                .deleted(false)
                .build();
        chatMessageRepository.save(welcomeMessage);

        SupportTicketDTO dto = mapper.toDTO(ticket);

        // Realtime signal svim employee klijentima da je stigao nov tiket —
        // zamena za polling liste tiketa na employee panelu.
        messagingTemplate.convertAndSend("/topic/employee/new-ticket", dto);

        return dto;
    }

    @Transactional(readOnly = true)
    public SupportTicketDTO getTicket(Long id, User currentUser){
        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        if (!isStaff(currentUser) && !supportTicket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Support ticket not found");
        }

        return mapper.toDTO(supportTicket);
    }

    private boolean isStaff(User user) {
        return user.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_MANAGER")
                        || authority.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    // null = MEDIUM (default); nevalidna vrednost se odbija — klijent ne može
    // poslati proizvoljan tekst kao hitnost.
    private TicketUrgency parseUrgency(String urgency) {
        if (urgency == null || urgency.isBlank()) {
            return TicketUrgency.MEDIUM;
        }
        try {
            return TicketUrgency.valueOf(urgency.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid ticket urgency: " + urgency);
        }
    }

    // Porudžbina mora postojati, ne sme biti obrisana i mora pripadati
    // pozivaocu — frontend ID se ne uzima na poverenje. 404 umesto 403 da se
    // ne otkriva postojanje tuđih porudžbina.
    private Order resolveLinkedOrder(Long orderId, Long userId) {
        if (orderId == null) {
            return null;
        }
        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketDTO> getAllTickets(Pageable pageable) {
        // Staff lista ne prikazuje tikete koje je staff arhivirao (uklonio iz liste);
        // vlasnik tiketa ih i dalje vidi kroz getUserTickets.
        return supportTicketRepository.findAllByDeletedFalseAndArchivedByStaffFalse(pageable)
                .map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketDTO> getUserTickets(Long userId, Pageable pageable) {
        return supportTicketRepository.findAllByUserIdAndDeletedFalse(userId, pageable)
                .map(mapper::toDTO);
    }

    @Transactional()
    public SupportTicketDTO updateTicketStatus(Long id, String status){
        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        try {
            supportTicket.setStatus(TicketStatus.valueOf(status.trim().replace("\"", "").toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid ticket status: " + status);
        }
        supportTicketRepository.save(supportTicket);
        return mapper.toDTO(supportTicket);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessages(Long sessionId, User currentUser){
        SupportTicket ticket = supportTicketRepository.findByChatSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        if (!isStaff(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Chat session not found");
        }

        return chatMessageRepository.findAllByChatSessionIdAndDeletedFalse(sessionId)
                .stream()
                .map(mapper::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public ChatMessageDTO sendMessage(Long sessionId, String content, String sender, User currentUser){
        SupportTicket ticket = supportTicketRepository.findByChatSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        if (!isStaff(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Chat session not found");
        }

        ChatSession chatSession = chatSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        MessageSender messageSender;
        try {
            messageSender = MessageSender.valueOf(sender);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid message sender: " + sender);
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .content(content)
                .sender(messageSender)
                .chatSession(chatSession)
                .build();

        chatMessageRepository.save(chatMessage);

        // Ako je staff ranije arhivirao tiket, nova poruka korisnika ga vraca u
        // staff listu - u suprotnom bi poruka ostala trajno nevidljiva za staff.
        if (messageSender == MessageSender.USER && ticket.isArchivedByStaff()) {
            ticket.setArchivedByStaff(false);
            supportTicketRepository.save(ticket);
            messagingTemplate.convertAndSend("/topic/employee/new-ticket", mapper.toDTO(ticket));
        }

        ChatMessageDTO dto = mapper.toMessageDTO(chatMessage);

        // Realtime broadcast svim pretplaćenim klijentima (customer + employee).
        // Broadcast je ovde, a ne u WS kontroleru, da bi i poruke poslate kroz
        // REST endpoint stizale realtime pretplaćenim klijentima.
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, dto);

        // Badge event za drugu stranu: employee poruka → customer topic,
        // customer poruka → zajednički employee topic. Zamena za REST polling
        // unread brojača.
        broadcastUnreadUpdate(ticket, sessionId, messageSender);

        return dto;
    }

    /**
     * Broadcast trenutnog unread brojača za tiket ka strani koja poruke PRIMA.
     * unreadCount se računa iz baze (broj nepročitanih poruka datog pošiljaoca
     * u sesiji), pa je event idempotentan — klijent samo prepiše vrednost.
     */
    private void broadcastUnreadUpdate(SupportTicket ticket, Long sessionId, MessageSender sender) {
        if (sender == MessageSender.BOT) {
            return;
        }
        int unreadCount = chatMessageRepository.countByChatSessionIdAndSenderAndIsReadFalse(sessionId, sender);
        UnreadUpdateDTO update = UnreadUpdateDTO.builder()
                .ticketId(ticket.getId())
                .sessionId(sessionId)
                .unreadCount(unreadCount)
                .build();

        if (sender == MessageSender.EMPLOYEE) {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + ticket.getUser().getId() + "/unread", update);
        } else {
            messagingTemplate.convertAndSend("/topic/employee/unread", update);
        }
    }

    @Transactional(readOnly = true)
    public List<BotQuestionDTO> getBotQuestions(){
        return botQuestionRepository.findAllByDeletedFalseOrderByOrderIndexAsc()
                .stream()
                .map(mapper::toQuestionDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public BotResponseDTO saveBotResponse(Long ticketId, Long questionId, String response, User currentUser){
        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        if (!isStaff(currentUser) && !supportTicket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Support ticket not found");
        }

        BotQuestion botQuestion = botQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bot question not found"));

        BotResponse botResponse = BotResponse.builder()
                .response(response)
                .botQuestion(botQuestion)
                .supportTicket(supportTicket)
                .build();

        botResponseRepository.save(botResponse);
        return mapper.toResponseDTO(botResponse);
    }

    @Transactional
    public void deleteTicket(Long ticketId, Long userId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Ticket not found");
        }

        ticket.setDeleted(true);
        supportTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long ticketId, Long userId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ChatSession session = ticket.getChatSession();
        if (session == null) return 0;

        String role = ticket.getUser().getId().equals(userId) ? "USER" : "EMPLOYEE";
        String senderToCount = role.equals("USER") ? "EMPLOYEE" : "USER";

        return chatMessageRepository.countByChatSessionIdAndSenderAndIsReadFalse(
                session.getId(), MessageSender.valueOf(senderToCount));
    }

    @Transactional
    public void markMessagesAsRead(Long sessionId, Long userId) {
        SupportTicket ticket = supportTicketRepository.findByChatSessionId(sessionId)
                .orElse(null);
        if (ticket == null) return;

        MessageSender senderToMark = ticket.getUser().getId().equals(userId)
                ? MessageSender.EMPLOYEE
                : MessageSender.USER;

        List<ChatMessage> messages = chatMessageRepository.findAllByChatSessionIdAndDeletedFalse(sessionId);
        messages.stream()
                .filter(msg -> msg.getSender() == senderToMark)
                .forEach(msg -> msg.setRead(true));
        chatMessageRepository.saveAll(messages);

        // Badge event ka strani koja je upravo pročitala poruke — bez ovoga bi
        // (posle uklanjanja pollinga) njen unread brojač ostao zamrznut na
        // staroj vrednosti dok ručno ne osveži listu. Kod employee čitanja
        // event ujedno sinhronizuje i ostale employee klijente.
        UnreadUpdateDTO update = UnreadUpdateDTO.builder()
                .ticketId(ticket.getId())
                .sessionId(sessionId)
                .unreadCount(0)
                .build();
        if (senderToMark == MessageSender.EMPLOYEE) {
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/unread", update);
        } else {
            messagingTemplate.convertAndSend("/topic/employee/unread", update);
        }
    }

    /**
     * Staff uklanja tiket iz staff liste (arhiviranje, ne brisanje) - vlasnik
     * tiketa i dalje vidi sopstvenu istoriju. Nova poruka vlasnika automatski
     * vraca tiket u staff listu (vidi sendMessage).
     */
    @Transactional
    public void archiveTicket(Long ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.setArchivedByStaff(true);
        supportTicketRepository.save(ticket);
    }

    /**
     * Unread brojaci grupisani po statusu tiketa, za badge na status filter
     * chipovima. Za vlasnika (customer pregled "moji tiketi") broje se
     * neprocitane EMPLOYEE poruke; za staff pregled svih tiketa broje se
     * neprocitane USER poruke (isti princip kao getUnreadCount). Racuna se
     * nad svim tiketima u bazi, ne nad jednom paginiranom stranicom.
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getMyUnreadSummary(Long userId) {
        return toSummaryMap(supportTicketRepository
                .countUnreadMessagesByStatusForUser(userId, MessageSender.EMPLOYEE));
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getStaffUnreadSummary() {
        return toSummaryMap(supportTicketRepository
                .countUnreadMessagesByStatusForStaff(MessageSender.USER));
    }

    private Map<String, Integer> toSummaryMap(List<Object[]> rows) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            summary.put(status.name(), 0);
        }
        for (Object[] row : rows) {
            summary.put(((TicketStatus) row[0]).name(), ((Long) row[1]).intValue());
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public int getTotalUnreadCount(Long userId) {
        List<SupportTicket> tickets = supportTicketRepository.findAllByUserIdAndDeletedFalse(userId);
        return tickets.stream()
                .filter(ticket -> ticket.getChatSession() != null)
                .mapToInt(ticket -> chatMessageRepository.countByChatSessionIdAndSenderAndIsReadFalse(
                        ticket.getChatSession().getId(), MessageSender.EMPLOYEE))
                .sum();
    }
}
