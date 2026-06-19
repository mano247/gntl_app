package com.gentlemanstore.discount.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiscountRequest {
    @NotBlank
    private String code;

    @NotNull
    private String discountType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal value;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validTo;

    private Long productId;

    private Long categoryId;
}
