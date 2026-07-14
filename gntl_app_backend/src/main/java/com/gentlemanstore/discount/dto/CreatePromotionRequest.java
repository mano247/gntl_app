package com.gentlemanstore.discount.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromotionRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    // Opciono — ako je prazno, sistem generise jedinstven kod.
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    @NotNull
    private String discountType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Value must be greater than 0")
    private BigDecimal value;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validTo;
}
