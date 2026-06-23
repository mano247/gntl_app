package com.gentlemanstore.loyalty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPointsRequest {
    @NotNull
    @Positive(message = "Points must be greater than zero")
    private Integer points;
    @NotBlank
    private String description;
}
