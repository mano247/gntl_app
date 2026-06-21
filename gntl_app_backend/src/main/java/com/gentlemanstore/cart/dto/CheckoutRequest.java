package com.gentlemanstore.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    @NotNull(message = "Address is required")
    private Long addressId;

    private String promoCode;
}