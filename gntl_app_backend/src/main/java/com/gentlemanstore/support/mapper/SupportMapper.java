package com.gentlemanstore.support.mapper;

import com.gentlemanstore.support.dto.BotQuestionDTO;
import com.gentlemanstore.support.dto.BotResponseDTO;
import com.gentlemanstore.support.dto.ChatMessageDTO;
import com.gentlemanstore.support.dto.SupportTicketDTO;
import com.gentlemanstore.support.model.BotQuestion;
import com.gentlemanstore.support.model.BotResponse;
import com.gentlemanstore.support.model.ChatMessage;
import com.gentlemanstore.support.model.SupportTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportMapper {
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "status", target = "status")
    SupportTicketDTO toDTO(SupportTicket ticket);

    @Mapping(source = "sender", target = "sender")
    ChatMessageDTO toMessageDTO(ChatMessage message);

    BotQuestionDTO toQuestionDTO(BotQuestion question);

    @Mapping(source = "botQuestion.question", target = "question")
    BotResponseDTO toResponseDTO(BotResponse response);
}
