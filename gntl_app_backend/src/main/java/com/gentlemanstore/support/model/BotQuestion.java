package com.gentlemanstore.support.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bot_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private boolean deleted = false;
}
