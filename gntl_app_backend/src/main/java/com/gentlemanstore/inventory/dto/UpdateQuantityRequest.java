package com.gentlemanstore.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuantityRequest {
    @NotNull(message = "Quantity is required")
    @Min(value = 0)
    private Integer quantity;
}
