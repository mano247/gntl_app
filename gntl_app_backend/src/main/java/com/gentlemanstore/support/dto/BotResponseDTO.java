package com.gentlemanstore.support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotResponseDTO {
    private Long id;
    private String response;
    private String question;
}
