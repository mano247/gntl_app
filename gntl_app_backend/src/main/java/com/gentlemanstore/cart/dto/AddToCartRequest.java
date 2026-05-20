package com.gentlemanstore.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Long productSizeId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
