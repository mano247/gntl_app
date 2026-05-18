package com.gentlemanstore.loyalty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPointsRequest {
    @NotNull
    private Integer points;
    @NotBlank
    private String description;
}
