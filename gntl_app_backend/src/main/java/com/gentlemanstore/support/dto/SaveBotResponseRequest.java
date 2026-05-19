package com.gentlemanstore.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveBotResponseRequest {
    @NotNull
    private Long ticketId;
    @NotNull
    private Long questionId;
    @NotBlank
    private String response;
}
