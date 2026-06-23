package com.gentlemanstore.support.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.support.dto.*;
import com.gentlemanstore.support.mapper.SupportMapper;
import com.gentlemanstore.support.model.*;
import com.gentlemanstore.support.repository.*;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final SupportMapper mapper;

    @Transactional
    public SupportTicketDTO createTicket(Long userId, CreateTicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ChatSession chatSession = ChatSession.builder()
                .deleted(false)
                .build();
        chatSessionRepository.save(chatSession);

        SupportTicket ticket = SupportTicket.builder()
                .subject(request.getSubject())
                .status(TicketStatus.OPEN)
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

        return mapper.toDTO(ticket);
    }

    @Transactional(readOnly = true)
    public SupportTicketDTO getTicket(Long id){
        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        return mapper.toDTO(supportTicket);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketDTO> getAllTickets(Pageable pageable) {
        return supportTicketRepository.findAllByDeletedFalse(pageable)
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

        supportTicket.setStatus(TicketStatus.valueOf(status));
        supportTicketRepository.save(supportTicket);
        return mapper.toDTO(supportTicket);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessages(Long sessionId){
        return chatMessageRepository.findAllByChatSessionIdAndDeletedFalse(sessionId)
                .stream()
                .map(mapper::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public ChatMessageDTO sendMessage(Long sessionId, String content, String sender){
        ChatSession chatSession = chatSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        ChatMessage chatMessage = ChatMessage.builder()
                .content(content)
                .sender(MessageSender.valueOf(sender))
                .chatSession(chatSession)
                .build();

        chatMessageRepository.save(chatMessage);
        return mapper.toMessageDTO(chatMessage);
    }

    @Transactional(readOnly = true)
    public List<BotQuestionDTO> getBotQuestions(){
        return botQuestionRepository.findAllByDeletedFalseOrderByOrderIndexAsc()
                .stream()
                .map(mapper::toQuestionDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public BotResponseDTO saveBotResponse(Long ticketId, Long questionId, String response){
        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

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
