package com.gentlemanstore.support.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.support.dto.*;
import com.gentlemanstore.support.mapper.SupportMapper;
import com.gentlemanstore.support.model.*;
import com.gentlemanstore.support.repository.*;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        return mapper.toDTO(ticket);
    }

    public SupportTicketDTO getTicket(Long id){
        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        return mapper.toDTO(supportTicket);
    }

    public List<SupportTicketDTO> getAllTickets(){
        return supportTicketRepository.findAllByDeletedFalse()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<SupportTicketDTO> getUserTickets(Long userId){
        return supportTicketRepository.findAllByUserIdAndDeletedFalse(userId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public SupportTicketDTO updateTicketStatus(Long id, String status){
        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        supportTicket.setStatus(TicketStatus.valueOf(status));
        supportTicketRepository.save(supportTicket);
        return mapper.toDTO(supportTicket);
    }

    public List<ChatMessageDTO> getMessages(Long sessionId){
        return chatMessageRepository.findAllByChatSessionIdAndDeletedFalse(sessionId)
                .stream()
                .map(mapper::toMessageDTO)
                .collect(Collectors.toList());
    }

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

    public List<BotQuestionDTO> getBotQuestions(){
        return botQuestionRepository.findAllByDeletedFalseOrderByOrderIndexAsc()
                .stream()
                .map(mapper::toQuestionDTO)
                .collect(Collectors.toList());
    }

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
}
