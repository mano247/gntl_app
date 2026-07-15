package com.gentlemanstore.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {
    @NotBlank
    private String subject;

    // Opciona veza ka porudžbini — servis proverava da porudžbina
    // pripada trenutno prijavljenom korisniku.
    private Long orderId;

    // LOW / MEDIUM / HIGH; null = MEDIUM, nevalidna vrednost se odbija.
    private String urgency;
}
