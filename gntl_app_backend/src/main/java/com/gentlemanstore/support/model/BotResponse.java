package com.gentlemanstore.support.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bot_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String response;

    @Column(nullable = false)
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_question_id", nullable = false)
    private BotQuestion botQuestion;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_ticket_id", nullable = false)
    private SupportTicket supportTicket;
}