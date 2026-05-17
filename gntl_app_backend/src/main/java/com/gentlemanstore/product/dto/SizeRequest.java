package com.gentlemanstore.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeRequest {
    @NotBlank(message = "Size is required")
    private String size;

    @NotNull(message = "Quantity is required")
    private Integer quantity;
}
