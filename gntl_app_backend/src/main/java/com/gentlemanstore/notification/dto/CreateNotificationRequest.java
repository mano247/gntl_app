package com.gentlemanstore.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String message;

    @NotBlank
    private String notificationType;

    @NotNull
    private Long userId;
}
