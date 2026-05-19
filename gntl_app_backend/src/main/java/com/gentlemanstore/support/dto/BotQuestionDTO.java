package com.gentlemanstore.support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotQuestionDTO {
    private Long id;
    private String question;
    private Integer orderIndex;
}
